package ru.yuksanbo.common.timings

//todo:
data class TimingMetrics(
        val maxTime: Long,
        val averageTime: Double,
        val percentile75th: Double,
        val percentile98th: Double,
        val percentile95th: Double,
        val percentile99th: Double,
        val percentile999th: Double
)