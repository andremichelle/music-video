package audio

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.core.UGen
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import org.openrndr.Program

interface AudioPlayback {
    fun play()

    fun seconds(): Double

    fun stop()
}

class AudioPlaybackNone(private val program: Program) : AudioPlayback {
    override fun play() {
    }

    override fun seconds(): Double {
        return program.seconds
    }

    override fun stop() {
    }
}

class AudioPlaybackStream(context: AudioContext, private val format: AudioFormat) : AudioPlayback, UGen(context, 2) {
    var position: Long = 0

    override fun play() {
        if (!context.isRunning) {
            context.start()
        }
    }

    override fun seconds(): Double {
        return position.toDouble() / format.sampleRate().toDouble()
    }

    override fun stop() {
        context.stop()
    }

    override fun calculateBuffer() {
        format.readChannelFloat(bufOut[0], 0, position)
        format.readChannelFloat(bufOut[1], 1, position)
        position += bufferSize
    }

    companion object {
        fun create(format: AudioFormat): AudioPlaybackStream {
            val context = AudioContext(
                JavaSoundAudioIO(256),
                256,
                IOAudioFormat(format.sampleRate().toFloat(), 16, 0, 2)
            )
            val playbackStream = AudioPlaybackStream(context, format)
            context.out.addInput(playbackStream)
            return playbackStream
        }
    }
}