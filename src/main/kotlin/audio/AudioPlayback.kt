package audio

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.IOAudioFormat
import net.beadsproject.beads.core.io.JavaSoundAudioIO
import net.beadsproject.beads.data.SampleManager
import net.beadsproject.beads.ugens.SamplePlayer

class AudioPlayback(samplePath: String) {
    private val context: AudioContext = AudioContext(
        JavaSoundAudioIO(512),
        512,
        IOAudioFormat(48000.0f, 16, 0, 2)
    )
    private val samplePlayer: SamplePlayer

    init {
        samplePlayer = SamplePlayer(context, SampleManager.sample(samplePath))
        context.out.addInput(samplePlayer)
    }

    fun play() {
        if (!context.isRunning) {
            context.start()
        }
        samplePlayer.start()
    }

    fun seconds(): Double {
        return samplePlayer.position / 1000.0
    }

    fun stop() {
        println("Stop")
        context.stop()
        samplePlayer.kill()
    }
}