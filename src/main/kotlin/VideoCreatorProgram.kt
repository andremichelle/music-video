import audio.*
import draw.FpsMeter
import draw.FpsMeter.Companion.draw
import draw.Hud
import draw.Hud.Circle.Companion.draw
import draw.Spectrum
import draw.Waveform
import net.TrackApi
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Matrix55
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
// flying hexagons

@Suppress("ConstantConditionIf")
fun main() {
    val audioPlaybackMode = false
    val videoCaptureMode = true

    application {
        configure {
            width = 960
            height = 540
            title = "Video Preview"
        }
        program {
            val scene: Scene = Scene.list[2]

            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }

            val wavPath = "/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/${scene.trackKey}.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val track = TrackApi.fetch(scene.trackKey).track

            val fpsMeter = FpsMeter()
            val fontTrack = loadFont("data/fonts/IBMPlexMono-Regular.ttf", 18.0)
            val fontHex = loadFont("data/fonts/Andale Mono.ttf", 7.0)
            val fontTimeCode = loadFont("data/fonts/Andale Mono.ttf", 11.0)
            val frame = loadImage("data/images/hud-frame.png")
            val txt = loadImage("data/images/txt.png")
            val atl = loadImage("data/images/audiotool.png")
            val hero = loadImage("data/images/hero.png")
            val cov = loadImage(track.cover())

            val heroColorMatrix = Matrix55(
                1.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 1.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.5, 0.0,
                0.0, 0.0, 0.0, 0.0, 1.0
            )

            if (videoCaptureMode) {
                // call this in terminal to mux audio into video
                println(
                    "ffmpeg -i ${Paths.get("tmp/${scene.trackKey}.mp4").toAbsolutePath()} -i ${wavFile.toPath()
                        .toAbsolutePath()} -c copy tmp/${scene.trackKey}.mkv"
                )
                extend(ScreenRecorder()) {
                    outputFile = "tmp/${scene.trackKey}.mp4"
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
            val transform = AudioTransform(wavFormat, 1024)

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

            val random = Random(scene.seed)
            val rgBa = ColorRGBa.fromHex(0xFFFFFF)
            val circles: List<Hud.Circle> = listOf(
                Hud.Circle(random, 5, 0.0, 24.0).move(384, 408),
                Hud.Circle(random, 6, 4.0, 24.0).move(384, 472),
                Hud.Circle(random, 6, 4.0, 24.0).move(640, 392),
                Hud.Circle(random, 5, 4.0, 16.0).move(640, 440),
                Hud.Circle(random, 6, 4.0, 24.0).move(640, 488),
                Hud.Circle(random, 6, 20.0, 40.0).move(884, 468)
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

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            extend {
                drawer.clear(ColorRGBa.BLACK)
                val playBackSeconds = if (videoCaptureMode) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, track.bpm)
                transform.advance(playBackSeconds)
                scene.shadertoy.render(window.size * window.scale, playBackSeconds, track.bpm)

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
                    drawer.text(track.authors(), 40.0, 332.0)
                    drawer.text(track.name, 40.0, 332.0 + 16.0)
                    drawer.fontMap = fontHex
                    drawer.fill = ColorRGBa.WHITE
                    val r = Random(floor(bars * 16.0).toInt())
                    for (y in 0..12) {
                        for (x in 0..2) {
                            drawer.text(
                                r.nextInt(0, 0x10000)
                                    .toString(16)
                                    .toUpperCase()
                                    .padStart(4, '0'), 680.0 + x * 20, 382.0 + y * 10
                            )
                        }
                    }
                    drawer.fontMap = fontTimeCode
                    drawer.text(frameCount.toString().padStart(7, '0'), 879.0, 384.0)
                    drawer.text(formatDuration(seconds.toInt()), 879.0, 384.0 + 12.0)
                    drawer.text(
                        (floor(bars) + 1).toInt().toString()
                            .padStart(3, '0')
                            .padStart(5, ' ') + "." + ((floor(bars * 4.0) % 4).toInt() + 1).toString(),
                        879.0,
                        384.0 + 24.0
                    )
                    val time = bars * 0.125
                    val value = (Hud.getTimeMapper(time.toInt()))(time)
                    drawer.drawStyle.clip = Rectangle(758.0, 376.0, 56.0, 128.0)
                    drawer.image(txt, 758.0, 376.0 - 384.0 * (value - floor(value)), 56.0, 512.0)
                    drawer.drawStyle.clip = null
                    drawer.drawStyle.colorMatrix = heroColorMatrix
                    drawer.image(
                        hero,
                        Rectangle(32.0 * (floor(bars * 4.0 * 7.0) % 7), 0.0, 32.0, 32.0),
                        Rectangle(886.0 - 16.0, 468.0 - 16.0, 32.0, 32.0)
                    )
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                val timedInterval = max(ceil(1.0 - mod(bars, 8.0)), 0.0)
                chromaticAberration.aberrationFactor =
                    0.5 + cos(bars * PI * 0.5).pow(64.0) * 8.0 * timedInterval
                chromaticAberration.apply(blurred, blurred)
                drawer.stroke = null
                drawer.fill = ColorRGBa(0.0, 0.0, 0.0, scene.backgroundAlpha)
                drawer.rectangle(24.0, 360.0, 912.0, 160.0)
                drawer.image(blurred)
                drawer.image(cov, 40.0, 376.0, 128.0, 128.0)
                drawer.fill = rgBa.opacify(0.4)
                drawer.stroke = null
                val htL = normDb(transform.peakDb(0)) * 125.0
                val htR = normDb(transform.peakDb(1)) * 125.0
                drawer.rectangle(578.0, 503.0 - htL, 5.0, htL)
                drawer.rectangle(594.0, 503.0 - htR, 5.0, htR)
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
                if (0.0 < alphaInv) {
                    drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
                    drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))
                    if (!videoCaptureMode) {
                        drawer.draw(fpsMeter, seconds)
                    }
                }
            }
        }
    }
}

private fun createWaveform() = Waveform(128.0, 40.0)

private fun createSpectrum() = Spectrum(26, 21, 4, 2, 1)