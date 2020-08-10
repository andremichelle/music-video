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

    fun fps(): Int
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
            execute = { frame -> secondsToBars(frame.seconds, frame.bpm) }
        }, 0x30609, 0.4)
        val TECHNO = MixSceneSetup("techno", ShaderToy.fromFile("data/shader/day-179.fs") {
            execute = { frame -> secondsToBars(frame.seconds, frame.bpm) * 2.0 }
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

    override fun fps(): Int {
        return 30
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

    override fun fps(): Int {
        return 60
    }

    @Suppress("unused")
    companion object {
        val iwd52a2x = TrackSceneSetup(
            "iwd52a2x",
            ShaderToy.fromFile("data/shader/artifact-at-sea.fs") {
                execute = { frame -> secondsToBars(frame.seconds, frame.bpm) * 2.0 }
            }, 0x30679, 0.2
        )
        val zfg71nr7n = TrackSceneSetup(
            "zfg71nr7n",
            ShaderToy.fromFile("data/shader/cube-fall.fs") {
                execute = { frame -> secondsToBars(frame.seconds, frame.bpm) / 2.0 }
            }, 0x30679, 0.4
        )
        val a3j9z52kpc = TrackSceneSetup(
            "6a3j9z52kpc",
            ShaderToy.fromFile("data/shader/uplifting.fs") {
                uniforms = { frame, shader -> shader.uniform("iPeak", 0.5f + frame.transform.peak().toFloat() * 0.5f) }
                execute = { frame -> secondsToBars(frame.seconds, frame.bpm) / 2.0 }
            }, 0x31679, 0.6
        )
        val q8h92m1b58ua = TrackSceneSetup(
            "q8h92m1b58ua",
            ShaderToy.fromFile("data/shader/day-179.fs") {
                execute = { frame -> secondsToBars(frame.seconds, frame.bpm) / 2.0 }
            }, 0x31677, 0.6
        )
        val nc6fa9gu = TrackSceneSetup(
            "nc6fa9gu",
            ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                execute = { frame -> secondsToBars(frame.seconds, frame.bpm) / 2.0 }
            }, 0x3126727, 0.6
        )
    }
}