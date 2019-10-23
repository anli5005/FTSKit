package dev.anli.ftskit

abstract class StockOrder(val ticker: String, val quantity: Int) {
    internal abstract val asRequest: FTSRequest
    abstract val completesInstantly: Boolean
    abstract fun inverse(): StockOrder?
}

abstract class FTSBasicOrder internal constructor(
        ticker: String,
        private val type: String,
        quantity: Int
): StockOrder(ticker, quantity) {
    override val asRequest: FTSRequest get() = FTSSubmitOrderRequest(ticker, type, quantity.toString())
    override val completesInstantly = false
}

class MarketBuyOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Cash Purchase", quantity) {
    override fun inverse(): StockOrder? = MarketSellOrder(ticker, quantity)
}

class MarketSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Cash Sale", quantity) {
    override fun inverse(): StockOrder? = MarketBuyOrder(ticker, quantity)
}

class MarginBuyOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Margin Purchase", quantity) {
    override fun inverse(): StockOrder? = MarginSellOrder(ticker, quantity)
}

class MarginSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Margin Sale", quantity) {
    override fun inverse(): StockOrder? = MarginBuyOrder(ticker, quantity)
}

class ShortSellOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Short Sell ", quantity) {
    override fun inverse(): StockOrder? = ShortCoverOrder(ticker, quantity)
}

class ShortCoverOrder(ticker: String, quantity: Int): FTSBasicOrder(ticker, "Short Cover", quantity) {
    override fun inverse(): StockOrder? = ShortSellOrder(ticker, quantity)
}

operator fun StockOrder.unaryMinus(): StockOrder? = inverse()

operator fun MarketBuyOrder.plus(order: MarketBuyOrder): MarketBuyOrder? =
        if (ticker == order.ticker) MarketBuyOrder(ticker, quantity + order.quantity) else null
operator fun MarketSellOrder.plus(order: MarketSellOrder): MarketSellOrder? =
        if (ticker == order.ticker) MarketSellOrder(ticker, quantity + order.quantity) else null
operator fun MarginBuyOrder.plus(order: MarginBuyOrder): MarginBuyOrder? =
        if (ticker == order.ticker) MarginBuyOrder(ticker, quantity + order.quantity) else null
operator fun MarginSellOrder.plus(order: MarginSellOrder): MarginSellOrder? =
        if (ticker == order.ticker) MarginSellOrder(ticker, quantity + order.quantity) else null
operator fun ShortSellOrder.plus(order: ShortSellOrder): ShortSellOrder? =
        if (ticker == order.ticker) ShortSellOrder(ticker, quantity + order.quantity) else null
operator fun ShortCoverOrder.plus(order: ShortCoverOrder): ShortCoverOrder? =
        if (ticker == order.ticker) ShortCoverOrder(ticker, quantity + order.quantity) else null