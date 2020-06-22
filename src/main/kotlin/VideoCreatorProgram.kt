import draw.Hud
import draw.Hud.Circle.Companion.draw
import draw.ShaderToy
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import kotlin.random.Random

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
            extend {
                shaderToy.render(window.size * window.scale, seconds * 0.25)
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.draw(listOf(circle), rgBa, seconds)
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)
            }
        }
    }
}