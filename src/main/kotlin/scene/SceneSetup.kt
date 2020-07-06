package scene

import audio.AudioFormat
import audio.AudioTransform
import audio.WavStream
import audio.secondsToBars
import draw.ShaderToy
import net.Playlist
import java.nio.file.Path
import java.nio.file.Paths

interface SceneSetup {
    fun duration(): Double

    fun createAudioFormat(): AudioFormat

    fun createAudioTransform(): AudioTransform

    fun printMuxCommand()

    fun mp4OutputPath(): String

    fun wavPath(): String
}

class MixSceneSetup(
    private val folder: String,
    val shadertoy: ShaderToy,
    val seed: Int,
    val backgroundAlpha: Double
) : SceneSetup {
    private val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/$folder/mix.wav")
    private val audioFormat: AudioFormat = createAudioFormat()
    private val playlist = Playlist.fetch(folder)

    @Suppress("unused")
    companion object {
        val EDM = MixSceneSetup("edm", ShaderToy.fromFile("data/shader/the-popular-shader.fs") {
            timing = { seconds, bpm -> secondsToBars(seconds, bpm) }
        }, 0x30609, 0.4)
        val TECHNO = MixSceneSetup("techno", ShaderToy.fromFile("data/shader/day-179.fs") {
            timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
        }, 0x36963, 0.6)
    }

    fun playlist(): Playlist {
        return playlist
    }

    override fun duration(): Double {
        return playlist.duration
    }

    override fun createAudioFormat(): AudioFormat {
        return WavStream.forFile(wavPath.toFile())
    }

    override fun createAudioTransform(): AudioTransform {
        return AudioTransform(audioFormat)
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

class TrackSceneSetup(
    val trackKey: String,
    val shadertoy: ShaderToy,
    val seed: Int,
    val backgroundAlpha: Double
) : SceneSetup {
    private val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav")
    private val audioFormat: AudioFormat = createAudioFormat()

    override fun duration(): Double {
        return audioFormat.seconds()
    }

    override fun createAudioFormat(): AudioFormat {
        return WavStream.forFile(wavPath.toFile())
    }

    override fun createAudioTransform(): AudioTransform {
        return AudioTransform(audioFormat, 1024)
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
            TrackSceneSetup(
                "you_won_t_understand",
                ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x306709, 0.6
            ),
            TrackSceneSetup(
                "ztdqgahfsdhdzwroap5c2zvkosfzyem",
                ShaderToy.fromFile("data/shader/clouds.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x6709, 0.1
            ),
            TrackSceneSetup(
                "iwd52a2x",
                ShaderToy.fromFile("data/shader/artifact-at-sea.fs") {
                    timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
                }, 0x30679, 0.2
            ),
            TrackSceneSetup(
                "love_fail",
                ShaderToy.fromFile("data/shader/the-inversion-machine.fs") {
                    timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
                }, 0x30679, 0.4
            )
        )
    }
}