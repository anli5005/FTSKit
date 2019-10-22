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
class TradingSession(val url: String, val case: String, var username: String, var password: String) {
    private val client = HttpClient { install(WebSockets) }

    private var _isConnected = false
    /** Whether the session is connected to FTS. **/
    val isConnected get() = _isConnected

    private val job = Job()
    private val scope = CoroutineScope(job)

    private var messages = ArrayDeque<String>()
    private var socketSession: ClientWebSocketSession? = null

    private val klaxon = Klaxon()

    private var continuations: MutableMap<KClass<out FTSMessage>, MutableList<Continuation<FTSMessage>>> = mutableMapOf()
    private var statusContinuations: ArrayDeque<Continuation<String>> = ArrayDeque()

    private var lastStatus: String? = null
    private var lastServerTime: String? = null
    /** The last server time received. **/
    val cachedServerTime: String? get() = lastServerTime

    private var _pendingOrders = LinkedList<PendingOrder<*>>()
    /** List of pending orders managed by this session. **/
    val pendingOrders get() = _pendingOrders.toList()

    /** Connects and logs in to FTS. **/
    fun connect() {
        if (!_isConnected) {
            _isConnected = true
            lastStatus = null
            scope.launch {
                while (_isConnected) {
                    client.webSocket(urlString = url) {
                        println("Connected!")

                        socketSession = this

                        launch {
                            // login()
                            while (!messages.isEmpty()) {
                                println("Sent ${messages.first}.")
                                send(Frame.Text(messages.removeFirst()))
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
                                    } else {
                                        // println("Ping received.")
                                    }
                                }

                                res?.msgList?.forEach { handle(it) }
                                res?.status?.let { handleStatus(it) }

                                res?.serverTime?.let { lastServerTime = it }
                            }
                        }
                    }

                    socketSession = null
                }
            }
        }
    }

    private suspend fun send(msg: String) {
        if (socketSession != null) {
            socketSession!!.send(Frame.Text(msg))
            println("Sent $msg.")
        } else {
            messages.add(msg)
            println("Added $msg.")
        }
    }

    private suspend fun send(request: FTSRequest) {
        send(klaxon.toJsonString(request))
    }

    private fun handle(message: FTSMessage) {
        when (message) {
            is FTSErrorMessage -> handleError(message)
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

    private fun handleError(msg: FTSErrorMessage) {
        TODO("Better error handling for ${msg.header}")
    }

    suspend fun login() {
        send(FTSLoginRequest(case, username, password))
        waitForStatus(FTSStatus.DONE)
    }

    /**
     Submits an order to FTS.
     **/
    suspend fun <TOrder: StockOrder>place(order: TOrder): PendingOrder<TOrder> {
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

    /* fun disconnect() {
        if (_isConnected) {
            _isConnected = false
        }
    } */

}