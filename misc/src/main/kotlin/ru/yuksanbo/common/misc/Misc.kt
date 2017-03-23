package ru.yuksanbo.common.misc

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Misc {

    val LOG: Logger = LoggerFactory.getLogger(Misc::class.java)

    fun systemExit(message: String = "", code: Int = 1): IllegalStateException {
        if (message.isNotEmpty()) {
            System.err.println(message)
        }
        System.exit(code)
        return IllegalStateException("System.exit didn't work")
    }

    inline fun benchmark(title: String, op: () -> Unit): Double {
        val started = System.nanoTime()
        op.invoke()
        val stopped = System.nanoTime()
        val taken = (stopped - started) / 1000000.0
        LOG.debug("{} taken {}ms", title, taken)
        return taken
    }
}

inline fun <T, R> letWith(receiver: T?, block: T.() -> R?): R? = receiver?.block()