import audio.*
import draw.*
import draw.FpsMeter.Companion.draw
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.shape.Rectangle
import java.io.File
import java.nio.file.Paths
import kotlin.math.pow
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)

// Todo
// How to draw a spectrum with one shader call (send height as uniform?)
// Add cover

@Suppress("ConstantConditionIf")
fun main() {
    val audioPlaybackMode = false
    val videoCaptureMode = false

    application {
        configure {
            width = 960
            height = 540
            title = "Video Preview"
        }
        program {
            val wavPath = "data/music/78qeujew8w.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val bpm = 126.0

            val fpsMeter = FpsMeter()
            val font = org.openrndr.draw.loadFont("data/fonts/IBMPlexMono-Regular.ttf", 18.0)
            val atl = loadImage("data/images/audiotool.png")
            val sBg = loadImage("data/images/hud-frame-spectrum.png")
            val wBg = loadImage("data/images/hud-frame-waveform.png")

            if (videoCaptureMode) {
                // call this in terminal to mux audio into video
                println(
                    "ffmpeg -i ${Paths.get("tmp/movie.mp4").toAbsolutePath()} -i ${wavFile.toPath()
                        .toAbsolutePath()} -c copy tmp/movie.mkv"
                )
                extend(ScreenRecorder()) {
                    outputFile = "tmp/movie.mp4"
                    quitAfterMaximum = true
                    maximumDuration = wavFormat.seconds()
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode) {
                    AudioPlaybackImpl(wavPath)
                } else {
                    AudioPlaybackNone(this)
                }
            val transform = AudioTransform(wavFormat)

            // Spectra
            val s0 = Spectrum(26, 21, 8, 4, 2)
                .background(sBg, Vector2(-22.0, -43.0), 0.25)
            val s1 = Spectrum(26, 21, 8, 4, 2)
                .background(sBg, Vector2(-22.0, -43.0), 0.25)
            s0.move(128, (height - s0.height()) / 2 - 64).reflect()
            s1.move((width - 128) - s1.width(), (height - s0.height()) / 2 - 64)

            // Waveforms
            val w0: Waveform = Waveform(282.0, 72.0)
                .background(wBg, Vector2(-18.0, -20.0), 0.25)
                .move(116, (height - s0.height()) / 2 + s0.height() + 32)

            val w1: Waveform = Waveform(282.0, 72.0)
                .background(wBg, Vector2(-18.0, -20.0), 0.25)
                .move(width - 116 - 282, (height - s0.height()) / 2 + s0.height() + 32)
                .reflect()

            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")

            val random = Random(0x303909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)

            val circles: List<Hud.Circle> = listOf(
                Hud.Circle(random, 6 + random.nextInt(7), 8.0, 48.0)
                    .move(width / 2, height / 2 - 128),
                Hud.Circle(random, 6 + random.nextInt(5), 8.0, 48.0)
                    .move(width / 2, height / 2 - 24),
                Hud.Circle(random, 6 + random.nextInt(7), 8.0, 48.0)
                    .move(width / 2, height / 2 + 128)
            )

            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }
            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            bloom.window = 25
            bloom.sigma = 1.0
            bloom.gain = 2.0

            val minDb: Double = -72.0
            val maxDb: Double = -0.0
            val normDb: (Double) -> Double = { db -> (db - minDb) / (maxDb - minDb) }

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            extend {
                val playBackSeconds = if (videoCaptureMode) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, bpm)
                transform.advance(playBackSeconds)
                shaderToy.render(window.size * window.scale, playBackSeconds) { shader ->
                    val value = normDb(transform.peakDb())
                    shader.uniform("iPeak", 0.125 + value.pow(16.0) * 0.25)
                }
                drawer.image(atl, (width - atl.width * 0.125) - 8.0, 8.0, atl.width * 0.125, atl.height * 0.125)
                drawer.isolatedWithTarget(rt) {
                    drawer.stroke = null
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    s0.draw(drawer, rgBa, transform, 0)
                    s1.draw(drawer, rgBa, transform, 1)

                    drawer.fill = rgBa
                    w0.render(drawer, transform.channel(0))
                    w1.render(drawer, transform.channel(1))

                    drawer.draw(circles, rgBa, bars * 2.0)
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)

                drawer.fill = rgBa.opacify(0.3)
                drawer.stroke = null
                val widthL = normDb(transform.peakDb(0)) * 256.0
                val widthR = normDb(transform.peakDb(1)) * 256.0
                drawer.rectangle(width / 2.0 + 4, height - 32.0, widthR, 8.0)
                drawer.rectangle(width / 2.0 - widthL - 4, height - 32.0, widthL, 8.0)

                drawer.fontMap = font
                drawer.fill = ColorRGBa.WHITE
                drawer.text("Kepz", 8.0, 16.0)
                drawer.text("minima 01", 8.0, 32.0)

                val fadeOutTime = 5.0
                val total = wavFormat.seconds()
                val alphaInv = clamp((playBackSeconds - (total - fadeOutTime)) / fadeOutTime, 0.0, 1.0)
                drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
                drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))

                if (!videoCaptureMode) {
                    drawer.draw(fpsMeter, seconds)
                }
            }
        }
    }
}