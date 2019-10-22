package dev.anli.ftskit

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.reflect.KClass

internal data class FTSResponse(
        val status: String,
        val msgList: List<FTSMessage>
)

internal class FTSMessageTypeAdapter: TypeAdapter<FTSMessage> {
    override fun classFor(type: Any): KClass<out FTSMessage> {
        return types[type as String] ?: FTSMessage::class
    }

    companion object {
        val types: Map<String, KClass<out FTSMessage>> = mapOf(
                "loginsuccess" to FTSLoginSuccessMessage::class,
                "loginerror" to FTSErrorMessage::class
        )
        val headers: Map<KClass<out FTSMessage>, String> = mapOf(*(types.entries.map { Pair(it.value, it.key) }.toTypedArray()))
    }
}

@TypeFor(field = "header", adapter = FTSMessageTypeAdapter::class)
internal open class FTSMessage(val header: String)
internal class FTSLoginSuccessMessage: FTSMessage(FTSMessageTypeAdapter.headers[FTSLoginSuccessMessage::class]!!)
internal class FTSErrorMessage(header: String): FTSMessage(header)