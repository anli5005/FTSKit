package dev.anli.ftskit

import com.beust.klaxon.Klaxon
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

/**
A session managing a connection to the FTS market simulator.
 **/
@KtorExperimentalAPI
class TradingSession(val url: String, val case: String, private var username: String, private var password: String) {
    private val client = HttpClient { install(WebSockets) }

    private var _isStarted = false
    /** Whether the session has been started. **/
    val isStarted get() = _isStarted

    /** Whether the session is connected to FTS. **/
    val isConnected get() = socketSession != null

    private var _isLoggedIn = false
    /** Whether the session is logged in to FTS. **/
    val isLoggedIn get() = _isLoggedIn

    private val job = Job()
    private val scope = CoroutineScope(job)

    private var socketSession: ClientWebSocketSession? = null

    private val klaxon = Klaxon()

    private var continuations: MutableMap<KClass<out FTSMessage>, MutableList<Continuation<FTSMessage>>> = mutableMapOf()
    private var statusContinuations: ArrayDeque<Continuation<String>> = ArrayDeque()
    private var connectContinuations: ArrayDeque<Continuation<Unit>> = ArrayDeque()
    private var loginContinuations: ArrayDeque<Continuation<Unit>> = ArrayDeque()

    private var lastStatus: String? = null
    private var lastServerTime: String? = null
    /** The last server time received. **/
    val cachedServerTime: String? get() = lastServerTime

    private var _pendingOrders = LinkedList<PendingOrder<*>>()
    /** List of pending orders managed by this session. **/
    val pendingOrders get() = _pendingOrders.toList()

    val tickerDatabase = FTSTickerDatabase()

    /** Connects to FTS. **/
    fun start() {
        if (!_isStarted) {
            _isStarted = true
            lastStatus = null
            scope.launch {
                while (_isStarted) {
                    client.webSocket(urlString = url) {
                        println("Connected!")

                        socketSession = this
                        while (connectContinuations.isNotEmpty()) {
                            connectContinuations.removeLast().resume(Unit)
                        }

                        launch {
                            login()
                            _isLoggedIn = true
                            while (loginContinuations.isNotEmpty()) {
                                loginContinuations.removeLast().resume(Unit)
                            }
                        }

                        while (true) {
                            val frame = incoming.receive()
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val res = klaxon.parse<FTSResponse>(text)

                                if (res != null) {
                                    if (lastStatus != res.status) {
                                        println("Status: ${res.status}")
                                        lastStatus = res.status
                                    }
                                }

                                res?.msgList?.forEach { handle(it) }
                                res?.apply { status?.let { handleStatus(it) } }

                                res?.serverTime?.let { lastServerTime = it }
                            }
                        }
                    }

                    socketSession = null
                    _isLoggedIn = false
                }
            }
        }
    }

    private suspend fun send(msg: String) {
        if (socketSession == null) {
            suspendCoroutine<Unit> { connectContinuations.add(it) }
        }

        socketSession!!.send(Frame.Text(msg))
        println("Sent $msg.")
    }

    private suspend fun send(request: FTSRequest) {
        send(klaxon.toJsonString(request))
    }

    private fun handle(message: FTSMessage) {
        when (message) {
            is FTSErrorMessage -> handleError(message)
            is FTSPositionGridMessage -> handlePositionGrid(message)
            is FTSGetTickersMessage -> Unit
            else -> println("Unrecognized message header: ${message.header}.")
        }

        continuations.remove(message::class)?.forEach { it.resume(message) }
    }

    private fun handleStatus(status: String) {
        while (statusContinuations.isNotEmpty()) {
            statusContinuations.removeLast().resume(status)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <TMessage: FTSMessage>waitFor(type: KClass<TMessage>): TMessage? {
        return suspendCoroutine<FTSMessage> {
            if (continuations[type] == null) {
                continuations[type] = mutableListOf(it)
            } else {
                continuations[type]!!.add(it)
            }
        } as TMessage
    }

    private suspend fun waitForStatus(where: (String) -> Boolean): String {
        var status: String? = lastStatus
        while (status == null || !where(status)) {
            status = suspendCoroutine { statusContinuations.add(it) }
        }
        return status
    }

    private suspend fun waitForStatus(status: String): String {
        return waitForStatus { it == status }
    }

    private suspend fun waitForStatus(status: FTSStatus): String {
        return waitForStatus(status.text)
    }

    suspend fun waitForLogin() {
        if (!_isLoggedIn) {
            suspendCoroutine<Unit> { loginContinuations.add(it) }
        }
    }

    private fun handleError(msg: FTSErrorMessage) {
        TODO("Better error handling for ${msg.header}")
    }

    private fun handlePositionGrid(msg: FTSPositionGridMessage) {
    }

    private suspend fun login() {
        send(FTSLoginRequest(case, username, password))
        waitForStatus(FTSStatus.DONE)
    }

    /**
    Submits an order to FTS.
     **/
    suspend fun <TOrder: StockOrder>place(order: TOrder): PendingOrder<TOrder> {
        waitForLogin()

        val pending = PendingOrder(order)
        if (!order.completesInstantly) {
            _pendingOrders.add(pending)
        }

        send(order.asRequest)

        if (order.completesInstantly) {
            pending.registerCompletion()
        }
        return pending
    }

    suspend fun waitForIdle() {
        waitForStatus { it == FTSStatus.DONE.text || it.isEmpty() }
    }

    /* fun disconnect() {
        if (_isConnected) {
            _isConnected = false
        }
    } */

    inner class FTSTickerDatabase internal constructor(): AbstractTickerDatabase() {
        private var isRefreshing = false
        private var refreshContinuations: ArrayDeque<Continuation<Unit>> = ArrayDeque()

        override var tickerToInfo: Map<String, TickerInfo> = mapOf()
        override var securityNameToInfo: Map<String, TickerInfo> = mapOf()

        suspend fun refreshDatabase() {
            if (isRefreshing) {
                suspendCoroutine<Unit> { refreshContinuations.add(it) }
            } else {
                isRefreshing = true

                waitForLogin()

                println("Refreshing ticker database...")

                var lastText: String? = null
                var page = 0
                var message: FTSGetTickersMessage? = null

                val tickers: MutableMap<String, TickerInfo> = mutableMapOf()
                val securityNames: MutableMap<String, TickerInfo> = mutableMapOf()

                while (message == null || lastText != message.text) {
                    lastText = message?.text
                    println("Refreshing ticker database... (Page ${page + 1})")
                    send(FTSGetTickersRequest(page * 100))
                    message = waitFor(FTSGetTickersMessage::class)

                    message?.parse()?.forEach {
                        tickers[it.ticker] = it
                        securityNames[it.securityName] = it
                    }

                    page++
                }

                tickerToInfo = tickers
                securityNameToInfo = securityNames

                isRefreshing = false
                while (refreshContinuations.isNotEmpty()) {
                    refreshContinuations.removeLast().resume(Unit)
                }

                println("Ticker database refreshed.")
            }
        }
    }

}