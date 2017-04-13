package ru.yuksanbo.common.timings


internal interface TimingsMXBean {

    fun getTimingsMetrics(): Map<String, TimingMetrics>

    fun resetMetrics()
}