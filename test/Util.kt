package dev.anli.ftskit.test

import dev.anli.ftskit.TradingSession
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun makeSession(): TradingSession {
    return TradingSession(dev.anli.ftskit.test.private.url, dev.anli.ftskit.test.private.case, dev.anli.ftskit.test.private.username, dev.anli.ftskit.test.private.password)
}