package dev.anli.ftskit

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.reflect.KClass

internal data class FTSResponse(
        val status: String,
        val msgList: List<FTSMessage>,
        val serverTime: String?
)

internal class FTSMessageTypeAdapter: TypeAdapter<FTSMessage> {
    override fun classFor(type: Any): KClass<out FTSMessage> {
        return types[type as String] ?: FTSMessage::class
    }

    companion object {
        val types: Map<String, KClass<out FTSMessage>> = mapOf(
                "loginerror" to FTSErrorMessage::class,
                "msgbox" to FTSMsgBoxMessage::class
        )
        val headers: Map<KClass<out FTSMessage>, String> = mapOf(*(types.entries.map { Pair(it.value, it.key) }.toTypedArray()))
    }
}

@TypeFor(field = "header", adapter = FTSMessageTypeAdapter::class)
internal open class FTSMessage(val header: String)
internal class FTSMsgBoxMessage: FTSMessage(FTSMessageTypeAdapter.headers[FTSMsgBoxMessage::class]!!)
internal class FTSErrorMessage(header: String): FTSMessage(header)

internal enum class FTSStatus {
    DONE;

    val text: String get() = when (this) {
        DONE -> "Connected to Server"
    }
}