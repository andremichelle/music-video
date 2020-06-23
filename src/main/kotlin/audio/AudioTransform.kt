package audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

class AudioTransform(val format: AudioFormat, val fftSize: Int) {
    val bandWidth = format.sampleRate() / fftSize.toDouble()
    val bins: FloatArray = FloatArray(fftSize shr 1)
    val buffer: FloatArray = FloatArray(fftSize)

    private val fft: FFT = FFT(fftSize)
    private val real: FloatArray = FloatArray(fftSize)
    private val imag: FloatArray = FloatArray(fftSize)
    private val window: FloatArray = FloatArray(fftSize)

    private var position: Int = 0
    private var peak = 0.0f

    fun next() {
        read(buffer, 0)
        for (i in 0 until fftSize) {
            val value = buffer[i]
            val abs = abs(value)
            if (peak < abs) {
                peak = abs
            } else {
                peak *= 0.99996f
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
            if (bins[i] <= energy) {
                bins[i] = energy
            } else {
                bins[i] *= 0.86f
            }
        }
        imag.fill(0f)
    }

    init {
        val a = PI / (fftSize - 1).toDouble()
        for (i in 0 until fftSize) window[i] =
            (0.42323 - 0.49755 * cos(2.0 * a * i) + 0.07922 * cos(4.0 * a * i)).toFloat()
    }

    private fun read(target: FloatArray, channelIndex: Int) {
        require(target.size == fftSize)
        format.readChannelFloat(target, channelIndex, position)
        position += fftSize
    }
}