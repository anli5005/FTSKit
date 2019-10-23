package dev.anli.ftskit

data class TickerInfo(
        val ticker: String,
        val securityName: String
)

interface TickerDatabase {
    fun infoForTicker(ticker: String): TickerInfo?
    fun infoForSecurityName(securityName: String): TickerInfo?
    fun allTickers(): List<TickerInfo>
}

abstract class AbstractTickerDatabase: TickerDatabase {
    override fun allTickers(): List<TickerInfo> {
        return tickerToInfo.values.toList()
    }

    protected abstract var tickerToInfo: Map<String, TickerInfo>
    protected abstract var securityNameToInfo: Map<String, TickerInfo>

    override fun infoForSecurityName(securityName: String): TickerInfo? {
        return securityNameToInfo[securityName]
    }

    override fun infoForTicker(ticker: String): TickerInfo? {
        return tickerToInfo[ticker]
    }
}

interface ListBasedTickerDatabase: TickerDatabase {
    val tickers: List<String>
    val securityNames: List<String>

    override fun infoForTicker(ticker: String): TickerInfo? {
        return tickers.indexOfOrNull(ticker)?.let { TickerInfo(ticker, securityNames[it]) }
    }

    override fun infoForSecurityName(securityName: String): TickerInfo? {
        return securityNames.indexOfOrNull(securityName)?.let { TickerInfo(tickers[it], securityName) }
    }

    override fun allTickers(): List<TickerInfo> {
        return (0 until tickers.size).map { TickerInfo(tickers[it], securityNames[it]) }
    }
}