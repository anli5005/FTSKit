package dev.anli.ftskit

import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PendingOrder<TOrder: StockOrder> internal constructor(val order: TOrder) {
    private var continuations = ArrayDeque<Continuation<Unit>>()

    internal fun registerCompletion() {
        _complete = true
        while (continuations.isNotEmpty()) {
            continuations.removeFirst().resume(Unit)
        }
    }

    suspend fun waitForCompletion() {
        if (!_complete) {
            suspendCoroutine<Unit> { continuations.add(it) }
        }
    }

    private var _complete = false
    val complete: Boolean get() = _complete
}