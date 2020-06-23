package audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// http://www.nayuki.io/res/free-small-fft-in-multiple-languages/Fft.java

class FFT(private val n: Int) {
    private val cosTable: FloatArray
    private val sinTable: FloatArray

    init {
        val halfN = n / 2
        cosTable = FloatArray(halfN)
        sinTable = FloatArray(halfN)
        for (i in 0 until halfN) {
            val angle = 2.0 * PI * i.toDouble() / n.toDouble()
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    fun transform(real: FloatArray, imag: FloatArray) {
        // Initialization
        require(real.size == imag.size) { "Mismatched lengths" }
        val levels = 31 - Integer.numberOfLeadingZeros(n) // Equal to floor(log2(n))
        // Bit-reversed addressing permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr 32 - levels
            if (j > i) {
                var temp: Float = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
        }
        // Cooley-Tukey decimation-in-time radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfsize = size / 2
            val tablestep: Int = n / size
            var i = 0
            while (i < n) {
                var j = i
                var k = 0
                while (j < i + halfsize) {
                    val tpre = real[j + halfsize] * cosTable[k] + imag[j + halfsize] * sinTable[k]
                    val tpim = -real[j + halfsize] * sinTable[k] + imag[j + halfsize] * cosTable[k]
                    real[j + halfsize] = real[j] - tpre
                    imag[j + halfsize] = imag[j] - tpim
                    real[j] += tpre
                    imag[j] += tpim
                    j++
                    k += tablestep
                }
                i += size
            }
            if (size == n) // Prevent overflow in 'size *= 2'
                break
            size *= 2
        }
    }
}