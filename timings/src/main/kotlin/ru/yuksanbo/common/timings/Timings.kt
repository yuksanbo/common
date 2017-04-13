package ru.yuksanbo.common.timings

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.yuksanbo.common.jmx.MXBeanIdentity
import ru.yuksanbo.common.jmx.MXBeans
import ru.yuksanbo.common.misc.toUpperCamelCase

import java.time.Duration
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

val defaultLog by lazy { LoggerFactory.getLogger(Timings::class.java) }

class Timings private constructor(
        private val context: String,
        private val thresholdMillis: Long,
        private val logger: Logger,
        metricsEnabled: Boolean,
        userMxBeanName: String?
) {

    private val readings: Deque<Reading> = ConcurrentLinkedDeque<Reading>()
    private val mxBeanName: String? = userMxBeanName ?:
            if (metricsEnabled) {
                context.toUpperCamelCase()
            }
            else {
                null
            }
    private val timingsMetricsMXBean: TimingsMXBeanImpl?

    init {
        timingsMetricsMXBean = mxBeanName?.let {
            timingsMxBeans.computeIfAbsent(
                    MXBeanIdentity("ru.yuksanbo.common.timings", "TimingsMetrics", it),
                    { identity ->
                        val timingsMxBean = TimingsMXBeanImpl()
                        MXBeans.registerMXBean(timingsMxBean, identity)
                        timingsMxBean
                    }
            )
        }
    }

    @JvmOverloads
    constructor(
            context: String,
            thresholdMillis: Long,
            logger: Logger = defaultLog,
            metricsEnabled: Boolean = false
    ) : this(context, thresholdMillis, logger, metricsEnabled, null)

    @JvmOverloads
    constructor(
            context: String,
            thresholdMillis: Long,
            logger: Logger = defaultLog,
            mxBeanName: String
    ): this(context, thresholdMillis, logger, true, mxBeanName)

    @JvmOverloads
    fun takeReadings(description: String, collect: Boolean = false) {
        val reading = Reading(description, System.nanoTime())
        if (collect) {
            timingsMetricsMXBean?.let {
                mxBean -> readings.peekLast()?.let { lastReading -> mxBean.updateMetric(reading, lastReading) }
            }
        }
        readings.add(reading)
    }

    fun report() {
        if (!logger.isInfoEnabled) return

        if (readings.isEmpty()) return

        val firstMark = readings.peek()
        val totalMillis: Long
        if (readings.size > 1) {
            totalMillis = (readings.peekLast().timeNanos - firstMark.timeNanos) / 1000000
        } else {
            totalMillis = -1L
        }

        val reportBuilder = StringBuilder()

        val goneOver = totalMillis > thresholdMillis

        if (goneOver) {
            reportBuilder.append("Timings for $context; taken=${totalMillis}ms, threshold=${thresholdMillis}ms")
        } else {
            if (logger.isDebugEnabled) {
                logger.debug("Timings for $context; taken=${totalMillis}ms")
            }
            return
        }

        var prevTime = firstMark.timeNanos
        for ((description, timeNanos) in readings) {
            reportBuilder.append("\n${(timeNanos - prevTime) / 1000000}ms   $description")
            prevTime = timeNanos
        }

        logger.info(reportBuilder.toString())
    }

    fun getProcessingDuration(): Duration {
        if (readings.size <= 1) {
            return Duration.ZERO
        }
        return Duration.ofNanos(readings.last.timeNanos - readings.first.timeNanos)
    }

}