import audio.AudioPlayback
import audio.AudioTransform
import audio.WavFormat
import audio.secondsToBars
import draw.*
import draw.Fps.Companion.draw
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.clamp
import org.openrndr.shape.Rectangle
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)

// Figure
// How to draw a spectrum with one shader call (send height as uniform?)

@Suppress("ConstantConditionIf")
fun main() {
    val videoCaptureMode = true

    application {
        configure {
            width = 1920 / 2
            height = 1080 / 2
            title = "Video Preview"
        }
        program {
            val wavPath = "data/music/78qeujew8w.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val bpm = 126.0

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

            val audioPlayback = AudioPlayback(wavPath)
            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            val transform = AudioTransform(wavFormat)
            val sw0 = SpectrumWall(32, 16, 10, 4, 1)
            val sw1 = SpectrumWall(32, 16, 10, 4, 1)
            sw0.reflect()
            sw0.move(32, (height - sw0.height()) / 2)
            sw1.move((width - 32) - sw1.width(), (height - sw0.height()) / 2)
            val waveform0: Waveform = Waveform(sw0.width().toDouble(), sw0.height().toDouble()).move(
                32,
                (height - sw0.height()) / 2 + sw0.height() + 32
            )
            val waveform1: Waveform = Waveform(sw1.width().toDouble(), sw1.height().toDouble()).move(
                (width - 32) - sw1.width(), (height - sw0.height()) / 2 + sw1.height() + 32
            )
            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")
            val random = Random(0x303808909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)
            val circleA = Hud.Circle(random, 6 + random.nextInt(5), 8.0, 48.0)
                .move(width / 2, height / 2 - 64)
            val circleB = Hud.Circle(random, 6 + random.nextInt(5), 8.0, 48.0)
                .move(width / 2, height / 2 + 64)
            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }
            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            bloom.window = 5
            bloom.sigma = 0.1
            bloom.gain = 2.0

            val minDb: Double = -72.0
            val maxDb: Double = -0.0
            val normDb: (Double) -> Double = { db -> (db - minDb) / (maxDb - minDb) }

            val fps = Fps()
            extend {
                val playBackSeconds = if (videoCaptureMode) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, bpm)
                transform.advance(playBackSeconds)
                shaderToy.render(window.size * window.scale, playBackSeconds * 2.0) { shader ->
                    shader.uniform("iPeak", 0.5 + normDb(transform.peakDb()) * 0.5)
                }
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.draw(listOf(circleA, circleB), rgBa, bars * 2.0)
                    drawer.stroke = null
                    sw0.draw(drawer, rgBa, transform, 0)
                    sw1.draw(drawer, rgBa, transform, 1)
                    drawer.fill = rgBa
                    waveform0.render(drawer, transform.channel(0))
                    waveform1.render(drawer, transform.channel(1))
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)

                drawer.fill = rgBa.opacify(0.6)
                drawer.stroke = null
                val widthL = normDb(transform.peakDb(0)) * 256.0
                val widthR = normDb(transform.peakDb(1)) * 256.0
                drawer.rectangle(width / 2.0 + 4, height - 32.0, widthR, 8.0)
                drawer.rectangle(width / 2.0 - widthL - 4, height - 32.0, widthL, 8.0)

                if (!videoCaptureMode) {
                    drawer.draw(fps, seconds)
                }

                val fadeOutTime = 5.0
                val total = wavFormat.seconds()
                val alphaInv = clamp((playBackSeconds - (total - fadeOutTime)) / fadeOutTime, 0.0, 1.0)

                drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
                drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))
            }
        }
    }
}