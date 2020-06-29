import audio.*
import draw.*
import draw.FpsMeter.Companion.draw
import draw.Hud.Circle.Companion.draw
import net.TrackApi
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.clamp
import org.openrndr.math.mod
import org.openrndr.shape.Rectangle
import java.io.File
import java.nio.file.Paths
import kotlin.math.*
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)
// https://github.com/openrndr/openrndr/blob/master/openrndr-openal/src/main/kotlin/AudioPlayer.kt#L23 (Shutdown hook)
// Check out the layer feature https://guide.openrndr.org/#/10_OPENRNDR_Extras/C11_Poisson_fills

// Todo
// How to draw a spectrum with one shader call https://www.shadertoy.com/view/WtlyDj
// Allow to place shadertoy arbitrary on stage
// Nerdy timecodes
// random text scroller
// flying hexagons

@Suppress("ConstantConditionIf")
fun main() {
    val trackKey = "ztdqgahfsdhdzwroap5c2zvkosfzyem"
    val audioPlaybackMode = false
    val videoCaptureMode = false

    application {
        configure {
            width = 960
            height = 540
            title = "Video Preview"
        }
        program {
            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }

            val wavPath = "/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val track = TrackApi.fetch(trackKey).track

            val fpsMeter = FpsMeter()
            val fontTrack = org.openrndr.draw.loadFont("data/fonts/IBMPlexMono-Regular.ttf", 18.0)
            val fontData = org.openrndr.draw.loadFont("data/fonts/Andale Mono.ttf", 12.0)
            val frame = loadImage("data/images/hud-frame.png")
            val atl = loadImage("data/images/audiotool.png")
            val cov = loadImage(track.cover())

            if (videoCaptureMode) {
                // call this in terminal to mux audio into video
                println(
                    "ffmpeg -i ${Paths.get("tmp/$trackKey.mp4").toAbsolutePath()} -i ${wavFile.toPath()
                        .toAbsolutePath()} -c copy tmp/$trackKey.mkv"
                )
                extend(ScreenRecorder()) {
                    outputFile = "tmp/$trackKey.mp4"
                    quitAfterMaximum = true
                    maximumDuration = wavFormat.seconds()
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode && !videoCaptureMode) {
                    AudioPlaybackImpl(wavPath)
                } else {
                    AudioPlaybackNone(this)
                }
            val transform = AudioTransform(wavFormat)

            // Spectra
            val s0 = createSpectrum()
            val s1 = createSpectrum()
            s0.move(216, 376).reflect()
            s1.move(424, 376)

            // Waveforms
            val w0: Waveform = createWaveform()
                .move(216, 464)
            val w1: Waveform = createWaveform()
                .move(424, 464)
                .reflect()

            val random = Random(0x306709)
            val rgBa = ColorRGBa.fromHex(0xFFFFFF)
            val circles: List<Hud.Circle> = listOf(
                Hud.Circle(random, 5, 0.0, 24.0).move(384, 408),
                Hud.Circle(random, 6, 4.0, 24.0).move(384, 472),
                Hud.Circle(random, 6, 4.0, 24.0).move(640, 392),
                Hud.Circle(random, 5, 4.0, 16.0).move(640, 440),
                Hud.Circle(random, 6, 4.0, 24.0).move(640, 488)
            )
            val rt = renderTarget(width, height) {
                colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
                depthBuffer()
            }
            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            bloom.window = 25
            bloom.sigma = 0.5
            bloom.gain = 1.0
            val chromaticAberration = ChromaticAberration()

            val normDb = normDb()

            val shaderToy = ShaderToy.fromFile("data/shader/clouds.fs")

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            extend {
                drawer.clear(ColorRGBa.BLACK)
                val playBackSeconds = if (videoCaptureMode) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, track.bpm)
                transform.advance(playBackSeconds)
                shaderToy.render(window.size * window.scale, playBackSeconds * 0.5) { shader ->
                    val value = normDb(transform.peakDb())
//                    shader.uniform("iPeak", 0.125 + value.pow(16.0) * 0.25)
//                    shader.uniform("iZoom", 1.2)
//                    shader.uniform("iRadius", 128.0)
                }

                drawer.image(atl, (width - atl.width * 0.125) - 8.0, 8.0, atl.width * 0.125, atl.height * 0.125)
                drawer.isolatedWithTarget(rt) {
                    drawer.stroke = null
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.image(frame, 0.0, 0.0, width.toDouble(), height.toDouble())
                    s0.draw(drawer, rgBa, transform, 0)
                    s1.draw(drawer, rgBa, transform, 1)

                    drawer.fill = rgBa
                    w0.render(drawer, transform.channel(0))
                    w1.render(drawer, transform.channel(1))

                    drawer.draw(circles, rgBa, bars * 2.0)

                    drawer.fontMap = fontTrack
                    drawer.fill = ColorRGBa.WHITE
                    drawer.text(track.collaborators.map { it.name }.joinToString(), 40.0, 340.0)
                    drawer.text(track.name, 40.0, 340.0 + 16.0)

                    drawer.fontMap = fontData
                    drawer.fill = ColorRGBa.WHITE
                    val r = Random(floor(bars * 16.0).toInt())
                    for (y in 0..7) {
                        for (x in 0..1) {
                            drawer.text(
                                r.nextLong(0, 0xFFFFFFFF + 1)
                                    .toString(16)
                                    .toUpperCase()
                                    .padStart(8, '0'), 686.0 + x * 60, 388.0 + y * 16
                            )
                        }
                    }
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                val timedInterval = max(ceil(1.0 - mod(bars, 8.0)), 0.0)
                chromaticAberration.aberrationFactor =
                    0.5 + cos(bars * PI * 0.5).pow(64.0) * 8.0 * timedInterval
                chromaticAberration.apply(blurred, blurred)
                drawer.image(blurred)
                drawer.image(cov, 40.0, 376.0, 128.0, 128.0)

                drawer.fill = rgBa.opacify(0.3)
                drawer.stroke = null
                val htL = normDb(transform.peakDb(0)) * 125.0
                val htR = normDb(transform.peakDb(1)) * 125.0
                drawer.rectangle(578.0, 503.0 - htL, 5.0, htL)
                drawer.rectangle(594.0, 503.0 - htR, 5.0, htR)

                drawer.fontMap = fontData
                drawer.text(frameCount.toString().padStart(7, '0'), 860.0, 424.0)
                drawer.text(formatDuration(seconds.toInt()), 860.0, 436.0)
                drawer.text(
                    (floor(bars) + 1).toInt().toString().padStart(3, '0') + "." + ((floor(bars * 4.0) % 4).toInt() + 1).toString(),
                    860.0,
                    448.0
                )

                val fadeInTime = 1.0
                val fadeOutTime = 5.0
                val total = wavFormat.seconds()
                val alphaInv = clamp(
                    (playBackSeconds - (total - fadeOutTime)) / fadeOutTime,
                    0.0,
                    1.0
                ) + clamp(
                    1.0 - playBackSeconds / fadeInTime,
                    0.0,
                    1.0
                )
                drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
                drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))

                if (!videoCaptureMode) {
                    drawer.draw(fpsMeter, seconds)
                }
            }
        }
    }
}

private fun createWaveform() = Waveform(128.0, 40.0)

private fun createSpectrum() = Spectrum(26, 21, 4, 2, 1)