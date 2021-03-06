package ru.yuksanbo.common.timings

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.StringWriter
import java.time.Duration
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class Timings
@JvmOverloads
constructor(
        private val context: String,
        private val thresholdMillis: Long,
        private val logger: Logger = LoggerFactory.getLogger(Timings::class.java)
) {

    private val readings: Deque<Reading> = ConcurrentLinkedDeque<Reading>()

    fun takeReading(description: String) {
        readings.add(Reading(description, System.nanoTime()))
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