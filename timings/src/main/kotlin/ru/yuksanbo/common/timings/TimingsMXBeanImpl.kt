package ru.yuksanbo.common.timings

import com.codahale.metrics.Reservoir
import com.codahale.metrics.SlidingTimeWindowReservoir
import ru.yuksanbo.common.jmx.MXBeanIdentity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal val timingsMxBeans: MutableMap<MXBeanIdentity, TimingsMXBeanImpl> = mutableMapOf()

internal class TimingsMXBeanImpl : TimingsMXBean {

    private val reservoirsMap = ConcurrentHashMap<String, Reservoir>()
    private val metricsMap = ConcurrentHashMap<String, TimingMetrics>()

    override fun getTimingsMetrics(): Map<String, TimingMetrics> {
        //todo: copy
        return metricsMap
    }

    override fun resetMetrics() {
        metricsMap.clear()
    }

    fun updateMetric(currentReading: Reading, previousReading: Reading) {
        val res = reservoirsMap.computeIfAbsent(currentReading.description, { SlidingTimeWindowReservoir(1, TimeUnit.SECONDS) })

        res.update((currentReading.timeNanos - previousReading.timeNanos) / 1000000)

        val snapshot = res.snapshot

        metricsMap.put(
                currentReading.description,
                TimingMetrics(
                        snapshot.max,
                        snapshot.median,
                        snapshot.get75thPercentile(),
                        snapshot.get98thPercentile(),
                        snapshot.get95thPercentile(),
                        snapshot.get99thPercentile(),
                        snapshot.get999thPercentile()
                )
        )
    }
}