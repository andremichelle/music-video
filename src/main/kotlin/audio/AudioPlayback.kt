package audio

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import net.beadsproject.beads.data.SampleManager
import net.beadsproject.beads.ugens.SamplePlayer
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

class AudioPlaybackImpl(samplePath: String) : AudioPlayback {
    private val context: AudioContext = AudioContext(
        JavaSoundAudioIO(512),
        512,
        IOAudioFormat(44100.0f, 16, 0, 2)
    )
    private val samplePlayer: SamplePlayer

    init {
        samplePlayer = SamplePlayer(context, SampleManager.sample(samplePath))
        context.out.addInput(samplePlayer)

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                println("shutdown")
                samplePlayer.kill()
                context.stop()
            }
        })
    }

    override fun play() {
        if (!context.isRunning) {
            context.start()
        }
        samplePlayer.start()
    }

    override fun seconds(): Double {
        return samplePlayer.position / 1000.0
    }

    override fun stop() {
        println("Stop")
        context.stop()
        samplePlayer.kill()
    }
}