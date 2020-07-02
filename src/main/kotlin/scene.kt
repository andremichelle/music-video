import audio.AudioFormatNull
import audio.AudioTransform
import audio.WavFormat
import audio.secondsToBars
import draw.ShaderToy
import net.Playlist
import java.nio.file.Path
import java.nio.file.Paths

interface Scene {
    fun duration(): Double

    fun createAudioTransform(): AudioTransform

    fun printMuxCommand()

    fun mp4OutputPath(): String

    fun wavPath(): String
}

class MixScene(val folder: String, val shadertoy: ShaderToy, val seed: Int, val backgroundAlpha: Double) : Scene {
    private val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/$folder/mix.wav")
    private val playlist = Playlist.fetch(folder)

    companion object {
        val list = listOf(
            MixScene("synth-wave", ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                timing = { seconds, _ -> seconds * 0.5 }
            }, 0x306709, 0.6)
        )
    }

    override fun duration(): Double {
        return playlist.duration
    }

    override fun createAudioTransform(): AudioTransform {
        return AudioTransform(AudioFormatNull())
    }

    override fun printMuxCommand() {
        println(
            "ffmpeg -i ${Paths.get("tmp/${folder}.mp4")
                .toAbsolutePath()} -i ${wavPath.toAbsolutePath()} -c copy tmp/${folder}.mkv"
        )
    }

    override fun mp4OutputPath(): String {
        return "tmp/${folder}.mp4"
    }

    override fun wavPath(): String {
        return wavPath.toAbsolutePath().toString()
    }
}

class TrackScene(val trackKey: String, val shadertoy: ShaderToy, val seed: Int, val backgroundAlpha: Double) : Scene {
    private val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav")
    private val wavFormat = WavFormat.decode(wavPath.toFile().readBytes())

    override fun duration(): Double {
        return wavFormat.seconds()
    }

    override fun createAudioTransform(): AudioTransform {
        return AudioTransform(wavFormat, 1024)
    }

    override fun printMuxCommand() {
        println(
            "ffmpeg -i ${Paths.get("tmp/${trackKey}.mp4")
                .toAbsolutePath()} -i ${wavPath.toAbsolutePath()} -c copy tmp/${trackKey}.mkv"
        )
    }

    override fun mp4OutputPath(): String {
        return "tmp/${trackKey}.mp4"
    }

    override fun wavPath(): String {
        return wavPath.toAbsolutePath().toString()
    }

    companion object {
        val list = listOf(
            TrackScene(
                "you_won_t_understand",
                ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x306709, 0.6
            ),
            TrackScene(
                "ztdqgahfsdhdzwroap5c2zvkosfzyem",
                ShaderToy.fromFile("data/shader/clouds.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x6709, 0.1
            ),
            TrackScene(
                "iwd52a2x",
                ShaderToy.fromFile("data/shader/artifact-at-sea.fs") {
                    timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
                }, 0x30679, 0.2
            ),
            TrackScene(
                "love_fail",
                ShaderToy.fromFile("data/shader/the-inversion-machine.fs") {
                    timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
                }, 0x30679, 0.4
            )
        )
    }
}