package audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

interface AudioFormat {
    fun seconds(): Double

    fun numChannels(): Int

    fun sampleRate(): Int

    fun readChannelFloat(target: FloatArray, channelIndex: Int, position: Long)
}

@Suppress("MemberVisibilityCanBePrivate")
open class WavHeader(
    val compression: Short,
    val numChannels: Short,
    val sampleRate: Int,
    val bytesPerSecond: Int,
    val blockAlign: Short,
    val bitsPerChannel: Short
) {
    override fun toString(): String {
        return "WavHeader(compression=$compression, " +
                "numChannels=$numChannels, " +
                "sampleRate=$sampleRate, " +
                "bytesPerSecond=$bytesPerSecond, " +
                "blockAlign=$blockAlign, " +
                "bitsPerChannel=$bitsPerChannel)"
    }
}

open class WavStream private constructor(
    private val header: WavHeader,
    private val dataOffset: Int,
    private val numFrames: Long,
    private val randomAccessFile: RandomAccessFile
) : AudioFormat {
    private val buffer = ByteBuffer.allocate(0xFFFF).order(ByteOrder.LITTLE_ENDIAN)

    override fun readChannelFloat(target: FloatArray, channelIndex: Int, position: Long) {
        val clampChannelIndex = min(channelIndex, numChannels() - 1)
        val n = min(target.size, (numFrames - position).toInt())
        var i = 0
        if (n > 0) {
            randomAccessFile.seek(dataOffset.toLong() + position * header.blockAlign)
            randomAccessFile.read(buffer.array(), 0, n * header.blockAlign)
            if (16 == header.bitsPerChannel.toInt()) {
                val shortBuffer = buffer.asShortBuffer()
                val scale = 1.0 / Short.MAX_VALUE.toDouble()
                while (i < n) {
                    val shortIndex = i * header.numChannels + clampChannelIndex
                    val shortValue = shortBuffer[shortIndex]
                    target[i] = (shortValue * scale).toFloat()
                    ++i
                }
            } else if (32 == header.bitsPerChannel.toInt()) {
                val floatBuffer = buffer.slice().asFloatBuffer()
                while (i < n) {
                    target[i] = floatBuffer[i * header.numChannels + clampChannelIndex]
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
        return numFrames / sampleRate().toDouble()
    }

    override fun numChannels(): Int {
        return header.numChannels.toInt()
    }

    override fun sampleRate(): Int {
        return header.sampleRate
    }

    override fun toString(): String {
        return "WavStream(header=$header)"
    }

    companion object {
        private const val MAGIC_RIFF = 0x46464952
        private const val MAGIC_WAVE = 0x45564157
        private const val MAGIC_FMT = 0x20746d66
        private const val MAGIC_DATA = 0x61746164

        fun forFile(file: File): AudioFormat {
            require(file.exists()) { "File does not exists (${file.toPath()})" }
            val randomAccessFile = RandomAccessFile(file, "r")
            val buffer = ByteBuffer.allocate(44)
            randomAccessFile.read(buffer.array())
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            require(buffer.int == MAGIC_RIFF) { "Expected RIFF" }
            buffer.int // file length
            require(buffer.int == MAGIC_WAVE) { "WAV does not start with WAVE" }
            require(buffer.int == MAGIC_FMT) { "First chunk must be FMT" }
            buffer.int // chunk length
            val compression = buffer.short
            val numChannels = buffer.short
            val sampleRate = buffer.int
            val bytesPerSecond = buffer.int
            val blockAlign = buffer.short
            val bitsPerChannel = buffer.short
            require(buffer.int == MAGIC_DATA) { "Second chunk must be DATA" }
            val length = buffer.int.toLong() and 0xFFFFFFFF
            val numFrames = length / blockAlign.toLong()
            val dataOffset = buffer.position()
            return WavStream(
                WavHeader(
                    compression,
                    numChannels,
                    sampleRate,
                    bytesPerSecond,
                    blockAlign,
                    bitsPerChannel
                ), dataOffset, numFrames, randomAccessFile
            )
        }
    }
}