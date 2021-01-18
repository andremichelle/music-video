package scene

import audio.AudioFormat
import audio.AudioTransform
import audio.WavStream
import audio.normDb
import draw.ShaderToy
import net.Playlist
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.pow

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
            execute = { frame -> frame.bars }
        }, 0x30609, 0.4)
        val TECHNO = MixSceneSetup("techno", ShaderToy.fromFile("data/shader/day-179.fs") {
            execute = { frame -> frame.bars * 2.0 }
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
            "ffmpeg -i ${
                Paths.get("tmp/${folder}.mp4")
                    .toAbsolutePath()
            } -i ${wavPath.toAbsolutePath()} -c copy tmp/${folder}.mkv"
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
    val backgroundAlpha: Double,
    val fps: Int
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
            "ffmpeg -i ${
                Paths.get("tmp/${trackKey}.mp4")
                    .toAbsolutePath()
            } -i ${wavPath.toAbsolutePath()} -c copy tmp/${trackKey}.mkv"
        )
    }

    override fun mp4OutputPath(): String {
        return "tmp/${trackKey}.mp4"
    }

    override fun wavPath(): String {
        return wavPath.toAbsolutePath().toString()
    }

    override fun fps(): Int {
        return fps
    }

    @Suppress("unused")
    companion object {
        val iwd52a2x = TrackSceneSetup(
            "iwd52a2x",
            ShaderToy.fromFile("data/shader/artifact-at-sea.fs") {
                execute = { frame -> frame.bars * 2.0 }
            }, 0x30679, 0.2, 60
        )
        val zfg71nr7n = TrackSceneSetup(
            "zfg71nr7n",
            ShaderToy.fromFile("data/shader/cube-fall.fs") {
                execute = { frame -> frame.bars / 2.0 }
            }, 0x30679, 0.4, 60
        )
        val a3j9z52kpc = TrackSceneSetup(
            "6a3j9z52kpc",
            ShaderToy.fromFile("data/shader/uplifting.fs") {
                uniforms = { frame, shader -> shader.uniform("iPeak", 0.5f + frame.transform.peak().toFloat() * 0.5f) }
                execute = { frame -> frame.bars / 2.0 }
            }, 0x31679, 0.6, 60
        )
        val q8h92m1b58ua = TrackSceneSetup(
            "q8h92m1b58ua",
            ShaderToy.fromFile("data/shader/day-179.fs") {
                execute = { frame -> frame.bars / 2.0 }
            }, 0x31677, 0.6, 60
        )
        val nc6fa9gu = TrackSceneSetup(
            "nc6fa9gu",
            ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                execute = { frame -> frame.bars / 2.0 }
            }, 0x3126727, 0.6, 60
        )
        val meqj2bteuqtg = TrackSceneSetup(
            "meqj2bteuqtg",
            ShaderToy.fromFile("data/shader/corridor.glsl") {
                execute = { frame -> frame.bars / 2.0 }
            }, 0x31267, 0.4, 60
        )
        val qr2d96umic = TrackSceneSetup(
            "5qr2d96umic",
            ShaderToy.fromFile("data/shader/tokio.glsl") {
                execute = { frame -> frame.bars }
            }, 0x3112267, 0.2, 60
        )
        val x8jyle44daw = TrackSceneSetup(
            "x8jyle44daw",
            ShaderToy.fromFile("data/shader/the-inversion-machine.fs") {
                uniforms = { frame, shader -> shader.uniform("iPeak", frame.transform.peak().toFloat().pow(4f)) }
                execute = { frame -> frame.bars }
            }, 0x311167, 0.3, 30
        )
        val wccwbqnwt = TrackSceneSetup(
            "0wccwbqnwt",
            ShaderToy.fromFile("data/shader/cube-field.glsl") {
                uniforms = { frame, shader -> shader.uniform("iPeak", frame.transform.peak().toFloat().pow(4f)) }
                execute = { frame -> frame.bars }
            }, 0x371267, 0.3, 30
        )
        val qunkrzyw2 = TrackSceneSetup(
            "0qunkrzyw2",
            ShaderToy.fromFile("data/shader/star-nest.glsl") {
                uniforms = { frame, shader -> shader.uniform("iPeak", frame.transform.peak().toFloat().pow(4f)) }
                execute = { frame -> frame.bars * 4.0 }
            }, 0x37117, 0.3, 30
        )
        val dxjrnzp196 = TrackSceneSetup(
            "dxjrnzp196",
            ShaderToy.fromFile("data/shader/warped-extruded.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0x371de17, 0.2, 30
        )
        val rbsxsxamp67p = TrackSceneSetup(
            "rbsxsxamp67p",
            ShaderToy.fromFile("data/shader/the-drive-home.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0x37ffe17, 0.2, 30
        )
        val ueutd1g771 = TrackSceneSetup(
            "ueutd1g771",
            ShaderToy.fromFile("data/shader/moist.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0x37ffe11, 0.1, 30
        )
        val family = TrackSceneSetup(
            "family",
            ShaderToy.fromFile("data/shader/happy-planet.glsl") {
                execute = { frame -> frame.bars }
            }, 0x37ffe08, 0.1, 30
        )
        val juetpcaxn4 = TrackSceneSetup(
            "51juetpcaxn4",
            ShaderToy.fromFile("data/shader/clouds.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0x37fe08, 0.1, 30
        )
        val wgwb2u5cbfh = TrackSceneSetup(
            "wgwb2u5cbfh",
            ShaderToy.fromFile("data/shader/generators.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0x7fe08, 0.1, 30
        )
        val saloxcs2nlydijtz90yxlfyubd6z5im = TrackSceneSetup(
            "saloxcs2nlydijtz90yxlfyubd6z5im",
            ShaderToy.fromFile("data/shader/sweeper.glsl") {
                execute = { frame -> frame.bars * 2.0 }
            }, 0x7de0a8, 0.4, 30
        )
        val iucid = TrackSceneSetup(
            "iucid",
            ShaderToy.fromFile("data/shader/abstract-terrain-objects.glsl") {
                execute = { frame -> frame.bars }
            }, 0x7de0a2, 0.3, 30
        )
        val timestopper = TrackSceneSetup(
            "timestopper",
            ShaderToy.fromFile("data/shader/messenger.glsl") {
                execute = { frame -> frame.bars * 2.0 }
            }, 0xfde0f2, 0.4, 30
        )
        val the_samuari_s_theme = TrackSceneSetup(
            "the_samuari_s_theme",
            ShaderToy.fromFile("data/shader/happy-jumping.glsl") {
                execute = { frame -> frame.bars * 2.0 }
            }, 0xfae0f2, 0.2, 30
        )
        val w28k1c9l = TrackSceneSetup(
            "w28k1c9l",
            ShaderToy.fromFile("data/shader/inercia.glsl") {
                execute = { frame -> frame.bars * 1.0 }
            }, 0xfae0fe, 0.5, 30
        )
        val ia9g13stx = TrackSceneSetup( // BROKEN MIXDOWN - NOT RELEASED YET
            "ia9g13stx",
            ShaderToy.fromFile("data/shader/silexars.glsl") {
                execute = { frame -> frame.bars * 1.0 }
            }, 0xf22fe, 0.5, 30
        )
        val khwnednc6 = TrackSceneSetup( // BROKEN MIXDOWN - NOT RELEASED YET
            "khwnednc6",
            ShaderToy.fromFile("data/shader/silexars.glsl") {
                execute = { frame -> frame.bars }
            }, 0xf2fe, 0.3, 30
        )
        val fwbse4jk2dq = TrackSceneSetup(
            "3fwbse4jk2dq",
            ShaderToy.fromFile("data/shader/flythrough.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0xf22fe, 0.3, 30
        )
        val wb8av7v2wp = TrackSceneSetup(
            "wb8av7v2wp",
            ShaderToy.fromFile("data/shader/thepheer.glsl") {
                val normDb = normDb(-36.0, -6.0)
                uniforms = { frame, shader ->
                    shader.uniform("lPeak", normDb.invoke(frame.transform.peakDb(0)).toFloat())
                    shader.uniform("rPeak", normDb.invoke(frame.transform.peakDb(1)).toFloat())
                }
                execute = { frame -> frame.bars }
            }, 0xf22fff, 0.3, 30
        )
        val njmd4gdeo = TrackSceneSetup(
            "njmd4gdeo",
            ShaderToy.fromFile("data/shader/synthwave.glsl") {
                uniforms = { frame, shader ->
                    shader.uniform("peak", frame.transform.peak().toFloat())
                }
                execute = { frame -> frame.bars * 3.0 }
            }, 0xfea22fe, 0.5, 30
        )
        val my1v82hik2 = TrackSceneSetup(
            "2my1v82hik2",
            ShaderToy.fromFile("data/shader/feast.glsl") {
                val normDb = normDb(-36.0, -3.0)
                uniforms = { frame, shader ->
                    shader.uniform("lPeak", normDb.invoke(frame.transform.peakDb(0)).toFloat())
                    shader.uniform("rPeak", normDb.invoke(frame.transform.peakDb(1)).toFloat())
                }
                execute = { frame -> frame.bars * 4.0 }
            }, 0xfeae2fe, 0.2, 30
        )
        val lxrtaavx9 = TrackSceneSetup(
            "lxrtaavx9",
            ShaderToy.fromFile("data/shader/day-376.glsl") {
                execute = { frame -> frame.bars * 2.0 }
            }, 0xffbe2fe, 0.4, 30
        )
        val enu06g1 = TrackSceneSetup(
            "1enu06g1",
            ShaderToy.fromFile("data/shader/brutalism.glsl") {
                execute = { frame -> frame.bars * 0.5 }
            }, 0xffbbcfe, 0.6, 30
        )
    }
}