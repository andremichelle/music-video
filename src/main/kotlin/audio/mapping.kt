package audio

import org.openrndr.math.clamp
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

fun secondsToNumFrames(seconds: Double, samplingRate: Int): Double {
    return seconds * samplingRate
}

fun secondsToBars(seconds: Double, bpm: Double): Double {
    return bpm * seconds / 240.0
}

fun barsToSeconds(bars: Double, bpm: Double): Double {
    return bars * 240.0 / bpm
}

private val DB_LV = ln(10.0) / 20.0

fun gainToDb(gain: Float): Double {
    return ln(gain) / DB_LV
}

fun normDb(minDb: Double = -72.0, maxDb: Double = 0.0): (Double) -> Double {
    return { db -> clamp((db - minDb) / (maxDb - minDb), 0.0, 1.0) }
}

fun formatDuration(seconds: Int): String {
    return String.format(
        "%d:%02d:%02d",
        abs(seconds) / 3600,
        abs(seconds) % 3600 / 60,
        abs(seconds) % 60
    )
}

class ExpStepper {
    var multiplier = 1.0
    var delta = 0.0

    private var y1 = 0.0
    private var y2 = 0.0
    private var x2 = 0.0
    private var dy = 0.0
    private var cstA = 0.0
    private var cstD = 0.0
    private var cstE = 0.0
    private var branch = 2
    fun slopeByHalf(y1: Double, ym: Double, y2: Double): Double {
        val yDiff = abs(y2 - y1)
        return if (Y_DIFF_MIN > yDiff) {
            0.5
        } else {
            (ym - y1) / (y2 - y1)
        }
    }

    fun byHalf(x2: Double, y1: Double, ym: Double, y2: Double) {
        bySlope(x2, y1, slopeByHalf(y1, ym, y2), y2)
    }

    fun bySlope(x2: Double, y1: Double, slope: Double, y2: Double) {
        val sv: Double = clamp(slope, SLOPE_LOWER, SLOPE_UPPER)
        dy = y2 - y1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
        if (sv > SLOPE_LINEAR_LOWER && sv < SLOPE_LINEAR_UPPER) {
            multiplier = 1.0
            delta = dy / x2
            branch = 2
        } else {
            val onemb = 1.0 - sv
            val onembs = onemb * onemb
            val onem2b = 1.0 - sv - sv
            val bends = sv * sv
            val s = onemb / sv
            multiplier = Math.pow(s, 2.0 / x2)
            val s2 = s * s
            delta = (y2 - y1 * s2) * (multiplier - 1.0) / (s2 - 1.0)
            cstA = (y1 * onembs - y2 * bends) / onem2b
            val B = dy * bends / onem2b
            cstD = 2.0 * Math.log(s) / x2
            cstE = Math.log(Math.abs(B))
            branch = if (B < 0) 1 else 0
        }
    }

    /**
     * Calculates y for a given x. X ranges from 0 to x2.
     *
     * @param x the position of which we want to know the y value
     */
    fun y(x: Double): Double {
        return when (branch) {
            0 -> {
                clamp(cstA + Math.exp(cstD * x + cstE), min(y1, y2), max(y1, y2))
            }
            1 -> {
                clamp(cstA - Math.exp(cstD * x + cstE), min(y1, y2), max(y1, y2))
            }
            else -> {
                y1 + x * dy / x2
            }
        }
    }

    fun x(y: Double): Double {
        return when (branch) {
            0 -> {
                (ln(y - cstA) - cstE) / cstD
            }
            1 -> {
                (ln(cstA - y) - cstE) / cstD
            }
            else -> {
                (y - y1) * x2 / dy
            }
        }
    }

    companion object {
        fun clampSlope(value: Double): Double {
            return clamp(value, SLOPE_LOWER, SLOPE_UPPER)
        }

        private const val SLOPE_LOWER = 0.000001
        private const val SLOPE_UPPER = 0.999999
        private const val SLOPE_LINEAR_LOWER = 0.499999
        private const val SLOPE_LINEAR_UPPER = 0.500001
        private const val Y_DIFF_MIN = 0.000001
    }
}