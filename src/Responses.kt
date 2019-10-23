package dev.anli.ftskit

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.reflect.KClass

internal data class FTSResponse(
        val status: String?,
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
                "positionGrid" to FTSPositionGridMessage::class,
                "getTickers" to FTSGetTickersMessage::class
        )
        val headers: Map<KClass<out FTSMessage>, String> = mapOf(*(types.entries.map { Pair(it.value, it.key) }.toTypedArray()))
    }
}

@TypeFor(field = "header", adapter = FTSMessageTypeAdapter::class)
internal open class FTSMessage(
        val header: String,
        val msg1: String?,
        val optionList: List<String>?
)

internal class FTSErrorMessage(header: String): FTSMessage(header, null, null)

internal class FTSPositionGridMessage(
        optionList: List<String>
): FTSMessage(
        FTSMessageTypeAdapter.headers[FTSPositionGridMessage::class]!!,
        null,
        optionList
) {
    fun parse(): List<Position> {
        return optionList!!.map { parseRow(it) }.map {
            Position(
                    it[0],
                    it[1].removing(",").toInt(),
                    it[2].removing(",").toIntBy100(),
                    it[3].removing(",").toIntBy100(),
                    it[4].removing(",").toIntBy100()
            )
        }
    }
}

internal class FTSGetTickersMessage(msg1: String): FTSMessage(
        FTSMessageTypeAdapter.headers[FTSGetTickersMessage::class]!!,
        msg1,
        null
) {
    val text get() = msg1!!

    fun parse(): List<TickerInfo> {
        return parseTable(text).map { TickerInfo(it[0], it[1]) }
    }
}

internal enum class FTSStatus {
    DONE;

    val text: String get() = when (this) {
        DONE -> "Connected to Server"
    }
}