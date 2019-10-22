package dev.anli.ftskit

abstract class StockOrder(val ticker: String, val quantity: Int) {
    internal abstract val asRequest: FTSRequest
    abstract val completesInstantly: Boolean
}

open class FTSBasicOrder internal constructor(ticker: String, private val type: String, quantity: Int): StockOrder(ticker, quantity) {
    override val asRequest: FTSRequest get() = FTSSubmitOrderRequest(ticker, type, quantity.toString())
    override val completesInstantly = false
}

class MarketBuyOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Cash Purchase", quantity)
class MarketSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Cash Sale", quantity)
class MarginBuyOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Margin Purchase", quantity)
class MarginSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Margin Sale", quantity)
class ShortSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Short Sell ", quantity)
class ShortCoverOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Short Cover", quantity)