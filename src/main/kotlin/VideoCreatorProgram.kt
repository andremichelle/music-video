import audio.AudioPlayback
import audio.AudioTransform
import audio.WavFormat
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
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)

// Figure
// How to draw a spectrum with one shader call (send height as uniform?)
// Mux audio into mp4 with ffmpeg after rendering video

@Suppress("ConstantConditionIf")
fun main() {
    application {
        configure {
            width = 1920 / 2
            height = 1080 / 2
            title = "Video Preview"
        }
        program {
            val renderVideo = false
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/movie.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 8.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }
            val DRAW_SHADERTOY = true
            val DRAW_HUD_CIRCLE = true
            val DRAW_SPECTRA = true

            val musicPath = "data/music/78qeujew8w.wav"
            val musicFile = File(musicPath)
            val audioPlayback = AudioPlayback(musicPath)
            val wavFormat = WavFormat.decode(musicFile.readBytes())
            val transform = AudioTransform(wavFormat)
            val spectrumWall0 = SpectrumWall(32, 24, 12, 4, 1).reflect()
            val spectrumWall1 = SpectrumWall(20, 16, 16, 4, 1)

            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")
            val random = Random(0x303808909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)
            val circle = Hud.Circle(random, 6 + random.nextInt(5), 8.0, 48.0)
                .move(width / 2.0, height / 2.0)
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
                val playBackSeconds = audioPlayback.seconds()
                transform.advance(playBackSeconds)
                if (DRAW_SHADERTOY) {
                    shaderToy.render(window.size * window.scale, playBackSeconds * 0.25)
                }
                if (DRAW_HUD_CIRCLE) {
                    drawer.isolatedWithTarget(rt) {
                        drawer.clear(ColorRGBa.TRANSPARENT)
                        drawer.draw(listOf(circle), rgBa, playBackSeconds)
                    }
                    bloom.apply(rt.colorBuffer(0), blurred)
                    drawer.image(blurred)
                }
                if (DRAW_SPECTRA) {
                    drawer.stroke = null
                    spectrumWall0.draw(drawer, transform, 0)
//                    drawer.pushTransforms()
//                    drawer.translate(width - spectrumWall1.width().toDouble(), 0.0)
//                    spectrumWall1.draw(drawer, transform, 1)
//                    drawer.popTransforms()
                }
                drawer.draw(fps, seconds)
            }
        }
    }
}