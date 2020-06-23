package audio

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

class AudioVisualMapping(
    private val transform: AudioTransform,
    private val minHz: Double,
    private val maxHz: Double,
    private val minDb: Double,
    private val maxDb: Double
) {
    private var position: Int = 0

    fun advance(targetTime: Double) {
        val targetFrame: Double = secondsToNumFrames(targetTime, transform.format.sampleRate())
        while (position < targetFrame) {
            transform.next()
            position += transform.fftSize
        }
    }

    fun write(normalized: FloatArray) {
        val bandWidth: Double = transform.bandWidth
        val bins: FloatArray = transform.bins
        var binIndex = 1
        for (c in normalized.indices) {
            val hx = (c + 1) / normalized.size.toDouble()
            val hz: Double = normToFreq(hx)
            val b1 = max(binIndex + 1, ceil(hz / bandWidth).toInt())
            var max = 0.0f
            while (binIndex < b1) {
                max = max(bins[binIndex++], max)
            }
            normalized[c] = dbToNorm(gainToDb(max)).toFloat()
        }
    }

    private fun normToFreq(x: Double): Double {
        return minHz * exp(x * ln(maxHz / minHz))
    }

    private fun dbToNorm(db: Double): Double {
        return (db - minDb) / (maxDb - minDb)
    }
}