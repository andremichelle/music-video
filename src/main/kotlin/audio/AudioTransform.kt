package audio

import kotlin.math.*

class AudioTransform(
    private val format: AudioFormat,
    private val fftSize: Int = 2048,
    private val minHz: Double = 30.0,
    private val maxHz: Double = 14000.0,
    private val minDb: Double = -60.0,
    private val maxDb: Double = -9.0,
    val fps: Double = 60.0
) {
    private val bandWidth = format.sampleRate() / fftSize.toDouble()
    private val spectra: List<FloatArray> = List(2) { FloatArray(fftSize shr 1) }
    private val channels: List<FloatArray> = List(2) { FloatArray(fftSize) }
    private val fft: FFT = FFT(fftSize)
    private val real: FloatArray = FloatArray(fftSize)
    private val imag: FloatArray = FloatArray(fftSize)
    private val window: FloatArray = FloatArray(fftSize)
    private val peaks = FloatArray(2)
    private val smoothingPeakCoeff = exp(-1.0 / (fps * 20.0)).toFloat() // 20000ms release-time
    private val smoothingEnergyCoeff = exp(-1.0 / (fps * 0.02)).toFloat() // 20ms release-time

    private var position: Long = 0

    fun advance(targetTime: Double) {
        when {
            0.0 == targetTime -> {
                next()
            }
            position + fftSize < format.numFrames() -> {
                val targetFrame: Double = secondsToNumFrames(targetTime, format.sampleRate())
                while (position < targetFrame) {
                    next()
                    position += fftSize
                }
            }
            else -> {
                for (channelIndex in 0..1) {
                    channels[channelIndex].fill(0f)
                    spectra[channelIndex].fill(0f)
                }
            }
        }
    }

    fun mapSpectrum(normalized: FloatArray, channelIndex: Int) {
        val bins: FloatArray = spectra[channelIndex]
        var binIndex = 1
        for (c in normalized.indices) {
            val hx = ((c + 1.0) / normalized.size).pow(8.0)
            val hz: Double = normToFreq(hx)
            val b1 = max(binIndex + 1, ceil(hz / bandWidth).toInt())
            var max = 0.0f
            while (binIndex < b1) {
                max = max(bins[binIndex++], max)
            }
            normalized[c] = dbToNorm(gainToDb(max)).toFloat()
        }
    }

    fun peakDb(): Double {
        var max = 0.0f
        for (peak in peaks) {
            max = max(peak, max)
        }
        return gainToDb(max)
    }

    fun peakDb(channelIndex: Int): Double {
        return gainToDb(peaks[channelIndex])
    }

    fun channel(channelIndex: Int): FloatArray {
        return channels[channelIndex]
    }

    private fun next() {
        for (channelIndex in 0..1) {
            val channel = channels[channelIndex]
            val spectrum = spectra[channelIndex]
            read(channel, channelIndex)
            for (i in 0 until fftSize) {
                val value = channel[i]
                val peak = abs(value)
                if (peaks[channelIndex] < peak && false) {
                    peaks[channelIndex] = peak
                } else {
                    peaks[channelIndex] = peak + smoothingPeakCoeff * (peaks[channelIndex] - peak)
                }
                real[i] = window[i] * value
            }
            fft.transform(real, imag)
            val numBins = fftSize shr 1
            val scale = 1.0 / numBins
            for (i in 0 until numBins) {
                val re = real[i]
                val im = imag[i]
                val energy = (sqrt(re * re + im * im) * scale).toFloat()
                if (spectrum[i] <= energy) {
                    spectrum[i] = energy
                } else {
                    spectrum[i] = energy + smoothingEnergyCoeff * (spectrum[i] - energy)
                }
            }
            imag.fill(0f)
        }
    }

    init {
        val a = PI / (fftSize - 1).toDouble()
        for (i in 0 until fftSize) window[i] =
            (0.42323 - 0.49755 * cos(2.0 * a * i) + 0.07922 * cos(4.0 * a * i)).toFloat()
    }

    private fun normToFreq(x: Double): Double {
        return minHz * exp(x * ln(maxHz / minHz))
    }

    private fun dbToNorm(db: Double): Double {
        return (db - minDb) / (maxDb - minDb)
    }

    private fun read(target: FloatArray, channelIndex: Int) {
        require(target.size == fftSize)
        format.readChannelFloat(target, channelIndex, position)
    }
}