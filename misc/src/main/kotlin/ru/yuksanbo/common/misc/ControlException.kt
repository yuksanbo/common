package ru.yuksanbo.common.misc

open class ControlException : RuntimeException {
    constructor() : super(null, null, false, false)
    constructor(message: String?) : super(message, null, false, false)
    constructor(message: String?, cause: Throwable?) : super(message, cause, false, false)
    constructor(cause: Throwable?) : super(null, cause, false, false)
}