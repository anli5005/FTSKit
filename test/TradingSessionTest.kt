package dev.anli.ftskit.test

import dev.anli.ftskit.*
import dev.anli.ftskit.data.FTS1000StocksCaseTickerDatabase
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

@KtorExperimentalAPI
class TradingSessionTest {
    @Test
    fun test() = runBlocking {
        val ticker = FTS1000StocksCaseTickerDatabase.infoForSecurityName("Apple, Inc.")!!.ticker

        val session = makeSession()
        session.start()
        session.place(MarketBuyOrder(ticker, 1))
        println("Test complete.")
    }
}