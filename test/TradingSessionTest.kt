package dev.anli.ftskit.test

import dev.anli.ftskit.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

@KtorExperimentalAPI
class TradingSessionTest {
    @Test
    fun test() = runBlocking {
        val ticker = "NFLX"

        val session = makeSession()
        session.connect()
        session.login()
        assert(session.isConnected)
        session.place(MarketBuyOrder(ticker, 1000))
        delay(60 * 1000)
        session.place(MarketSellOrder(ticker, 1000))
        delay(10000)
        println("Test complete.")
    }
}