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
        val session = makeSession()
        session.connect()
        assert(session.isConnected)
        session.login()
        session.place(MarketBuyOrder("BC", 1))
        session.place(ShortSellOrder("BC", 1))
        session.place(MarginBuyOrder("BC", 1))
        session.place(MarketSellOrder("BC", 1))
        session.place(ShortCoverOrder("BC", 1))
        session.place(MarginSellOrder("BC", 1))
        delay(2000) // Wait for orders to process
        println("Test complete.")
    }
}