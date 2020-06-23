import audio.AudioTransform
import audio.AudioVisualMapping
import audio.WavFormat
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

// https://www.shadertoy.com/view/ls3Xzf (Glitch)

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
            val wavFormat = WavFormat.decode(File("data/mixdowns/gmo6wo8484.wav").readBytes())
            val transform = AudioTransform(wavFormat, 2048)
            val audioVisualMapper = AudioVisualMapping(transform, 20.0, 20000.0, -60.0, -9.0)
            val spectrumWall = SpectrumWall(64, 32, 8, 4, 1)

            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")
            val random = Random(0x303808909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)
            println("${rgBa.r}, ${rgBa.g}, ${rgBa.b}")
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
            extend {
                audioVisualMapper.advance(seconds)
                shaderToy.render(window.size * window.scale, seconds * 0.25)
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.draw(listOf(circle), rgBa, seconds)
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)

                drawer.stroke = null
                spectrumWall.draw(drawer, audioVisualMapper)
            }
        }
    }
}