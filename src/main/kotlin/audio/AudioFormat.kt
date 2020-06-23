package audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

interface AudioFormat {
    fun seconds(): Double

    fun numChannels(): Int

    fun sampleRate(): Int

    fun readChannelFloat(target: FloatArray, channelIndex: Int, position: Int)
}

open class WavFormat private constructor(
    private val buffer: ByteBuffer,
    private val compression: Short,
    private val numChannels: Short,
    private val sampleRate: Int,
    private val bytesPerSecond: Int,
    private val blockAlign: Short,
    private val bitsPerChannel: Short,
    private val numFrames: Int,
    private val dataOffset: Int
) : AudioFormat {
    override fun readChannelFloat(target: FloatArray, channelIndex: Int, position: Int) {
        val clampChannelIndex = min(channelIndex, numChannels - 1)
        val n = min(target.size, numFrames - position)
        var i = 0
        if (n > 0) {
            buffer.position(dataOffset + position * blockAlign)
            if (16 == bitsPerChannel.toInt()) {
                val shortBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val scale = 1.0 / Short.MAX_VALUE.toDouble()
                while (i < n) {
                    val shortIndex = i * numChannels + clampChannelIndex
                    val shortValue = shortBuffer[shortIndex]
                    target[i] = (shortValue * scale).toFloat()
                    ++i
                }
            } else if (32 == bitsPerChannel.toInt()) {
                val floatBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                while (i < n) {
                    target[i] = floatBuffer[i * numChannels + clampChannelIndex]
                    ++i
                }
            }
        }
        while (i < target.size) {
            target[i] = 0f
            ++i
        }
    }

    override fun seconds(): Double {
        return numFrames / sampleRate.toDouble()
    }

    override fun numChannels(): Int {
        return numChannels.toInt()
    }

    override fun sampleRate(): Int {
        return sampleRate
    }

    override fun toString(): String {
        return "{Wav" +
                " buffer:" + (buffer.capacity() shr 10) + "k" +
                ", compression:" + compression +
                ", numChannels:" + numChannels +
                ", sampleRate:" + sampleRate +
                ", bytesPerSecond:" + bytesPerSecond +
                ", blockAlign:" + blockAlign +
                ", bitsPerChannel:" + bitsPerChannel +
                ", dataOffset:" + dataOffset +
                ", numFrames:" + numFrames +
                "}"
    }

    companion object {
        fun decode(bytes: ByteArray): WavFormat {
            val buffer = ByteBuffer.wrap(bytes)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            require(buffer.int == MAGIC_RIFF) { "Expected RIFF" }
            require(bytes.size == buffer.int + 8) { "WAV length does not match" }
            require(buffer.int == MAGIC_WAVE) { "WAV does not start with WAVE" }
            var compression: Short = -1
            var numChannels: Short = -1
            var sampleRate = -1
            var bytesPerSecond = -1
            var blockAlign: Short = -1
            var bitsPerChannel: Short = -1
            var dataOffset = -1
            var numFrames = -1
            var header = false
            var data = false
            while (8 < buffer.remaining()) {
                val id = buffer.int
                val length = buffer.int
                val position = buffer.position()
                if (id == MAGIC_FMT) {
                    compression = buffer.short
                    numChannels = buffer.short
                    sampleRate = buffer.int
                    bytesPerSecond = buffer.int
                    blockAlign = buffer.short
                    bitsPerChannel = buffer.short
                    header = true
                } else if (id == MAGIC_DATA) {
                    assert(-1 != blockAlign.toInt()) { "Header has not been read" }
                    dataOffset = position
                    numFrames = length / blockAlign
                    data = true
                }
                if (header && data) {
                    return WavFormat(
                        buffer, compression, numChannels,
                        sampleRate, bytesPerSecond, blockAlign,
                        bitsPerChannel, numFrames, dataOffset
                    )
                }
                buffer.position(position + length)
            }
            throw IllegalArgumentException("Unknown wav-format")
        }

        private const val MAGIC_RIFF = 0x46464952
        private const val MAGIC_WAVE = 0x45564157
        private const val MAGIC_FMT = 0x20746d66
        private const val MAGIC_DATA = 0x61746164
    }
}