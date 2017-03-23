package ru.yuksanbo.common.logging

import org.slf4j.Logger

inline fun Logger.debug(op: () -> String) {
    if (!isDebugEnabled) return
    debug(op.invoke())
}

inline fun Logger.debug(op: () -> String, t: Throwable) {
    if (!isDebugEnabled) return
    debug(op.invoke(), t)
}