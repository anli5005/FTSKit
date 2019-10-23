package dev.anli.ftskit

import kotlin.math.roundToInt

data class Position(val securityName: String, val position: Int, val lastValue: Int, val positionValue: Int, val gainLoss: Int)

fun Position.ticker(tickerDatabase: TickerDatabase): String? {
    return tickerDatabase.infoForSecurityName(securityName)?.ticker
}

fun Position.orders(ticker: String, includeMarginBuys: Boolean = true): List<StockOrder> {
    return when {
        position > 0 -> {
            if (includeMarginBuys) {
                val marginBuys = (2 * position * (1 - positionValue.toDouble() / lastValue.toDouble())).roundToInt()

                when {
                    marginBuys < 1 -> listOf(MarketBuyOrder(ticker, position))
                    position <= marginBuys -> listOf(MarginBuyOrder(ticker, marginBuys))
                    else -> listOf(MarketBuyOrder(ticker, position - marginBuys), MarginBuyOrder(ticker, marginBuys))
                }
            } else {
                listOf(MarketBuyOrder(ticker, position))
            }
        }
        position < 0 -> listOf(ShortSellOrder(ticker, -position))
        else -> listOf()
    }
}

fun Position.orders(tickerDatabase: TickerDatabase, includeMarginBuys: Boolean = true): List<StockOrder>? {
    return ticker(tickerDatabase)?.let { orders(it, includeMarginBuys) }
}