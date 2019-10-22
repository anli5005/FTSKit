package dev.anli.ftskit

import com.beust.klaxon.Klaxon
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.javaType

@KtorExperimentalAPI
class TradingSession(val url: String, val case: String, var username: String, var password: String) {
    private val client = HttpClient { install(WebSockets) }

    private var _isConnected = false
    val isConnected get() = _isConnected

    private val job = Job()
    private val scope = CoroutineScope(job)

    private var messages = ArrayDeque<String>()
    private var socketSession: ClientWebSocketSession? = null

    private val klaxon = Klaxon()

    private var continuations: MutableMap<KClass<out FTSMessage>, MutableList<Continuation<FTSMessage>>> = mutableMapOf()

    fun connect() {
        if (!_isConnected) {
            _isConnected = true
            scope.launch {
                while (_isConnected) {
                    client.webSocket(urlString = url) {
                        println("Connected!")

                        socketSession = this

                        while (!messages.isEmpty()) {
                            println("Sent ${messages.first}.")
                            send(Frame.Text(messages.removeFirst()))
                        }

                        while (true) {
                            val frame = incoming.receive()
                            if (frame is Frame.Text) {
                                val res = klaxon.parse<FTSResponse>(frame.readText())
                                if (res?.msgList.isNullOrEmpty()) {
                                    println("Ping received.")
                                } else {
                                    println("Received ${res!!.msgList!!.size} messages.")
                                }
                                res?.msgList?.forEach { handle(it) }
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
            is FTSLoginSuccessMessage -> handleLoginSuccess(message)
            is FTSErrorMessage -> handleError(message)
            else -> println("Unrecognized message header: ${message.header}.")
        }

        continuations.remove(message::class)?.forEach { it.resume(message) }
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

    private fun handleLoginSuccess(_msg: FTSLoginSuccessMessage) {
        println("Login succeeded.")
    }

    private fun handleError(msg: FTSErrorMessage) {
        TODO("Better error handling for ${msg.header}")
    }

    suspend fun login() {
        send(FTSLoginRequest(case, username, password))
        waitFor(FTSLoginSuccessMessage::class)
    }

    suspend fun place(order: StockOrder) {
        send(order.asRequest)
    }

    /* fun disconnect() {
        if (_isConnected) {
            _isConnected = false
        }
    } */

}