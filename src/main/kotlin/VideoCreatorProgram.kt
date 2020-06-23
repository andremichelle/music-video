import audio.AudioPlayback
import audio.AudioTransform
import audio.WavFormat
import audio.secondsToBars
import draw.Fps
import draw.Fps.Companion.draw
import draw.Hud
import draw.Hud.Circle.Companion.draw
import draw.ShaderToy
import draw.SpectrumWall
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)

// Figure
// How to draw a spectrum with one shader call (send height as uniform?)

@Suppress("ConstantConditionIf")
fun main() {
    application {
        configure {
            width = 1920 / 2
            height = 1080 / 2
            title = "Video Preview"
        }
        program {
            val DRAW_SHADERTOY = true
            val DRAW_HUD_CIRCLE = true
            val DRAW_SPECTRA = true
            val VIDEO_CAPTURE = false

            val wavPath = "data/music/78qeujew8w.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val bpm = 126.0

            if (VIDEO_CAPTURE) {
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
            if (!VIDEO_CAPTURE) {
                audioPlayback.play()
            }
            val transform = AudioTransform(wavFormat)
            val spectrumWall0 = SpectrumWall(20, 16, 12, 4, 1).reflect()
            val spectrumWall1 = SpectrumWall(20, 16, 16, 4, 1)
            spectrumWall0.move(0, height / 2)
            spectrumWall1.move(width - spectrumWall1.width(), height / 2)

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

            val fps = Fps()
            extend {
                val playBackSeconds = if (VIDEO_CAPTURE) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, bpm)
                transform.advance(playBackSeconds)

                if (DRAW_SHADERTOY) {
                    shaderToy.render(window.size * window.scale, playBackSeconds * 0.25)
                }
                drawer.isolatedWithTarget(rt) {
                    if (DRAW_HUD_CIRCLE) {
                        drawer.clear(ColorRGBa.TRANSPARENT)
                        drawer.draw(listOf(circleA, circleB), rgBa, bars * 2.0)
                    }
                    if (DRAW_SPECTRA) {
                        drawer.stroke = null
                        spectrumWall0.draw(drawer, transform, 0)
                        spectrumWall1.draw(drawer, transform, 1)
                    }
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)
                if (!VIDEO_CAPTURE) {
                    drawer.draw(fps, seconds)
                }
            }
        }
    }
}