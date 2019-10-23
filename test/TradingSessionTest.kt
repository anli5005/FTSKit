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
        val session = makeSession()
        session.start()
        session.waitForLogin()
        println("Connected.")

        val positions = session.waitForPositions()
        println("Positions: $positions")

        session.waitForIdle()
        session.end()
        println("Done!")
    }
}