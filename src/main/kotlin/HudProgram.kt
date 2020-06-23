import draw.Cross
import draw.Cross.Companion.draw
import draw.Hud
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.ffmpeg.ScreenRecorder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 752
            height = 752
            title = "Hud"
        }
        program {
            val renderVideo = false
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/movie.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 16.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val random = Random(0x303808909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)

            val circles: List<Hud.Circle> = List(25) { index ->
                val c = index % 5
                val r = index / 5
                val x = 88 + c * (128 + 16)
                val y = 88 + r * (128 + 16)
                Hud.Circle(random, 8 + random.nextInt(5), 16.0, 64.0)
                    .move(x, y)
            }

            val crosses: List<Cross> = List(25) { index ->
                val c = index % 5
                val r = index / 5
                val x = 88 + c * (128 + 16)
                val y = 88 + r * (128 + 16)
                Cross(4.0).move(x, y)
            }

            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }

            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            val chromaticAberration = ChromaticAberration()
            bloom.window = 1
            bloom.sigma = 20.0
            bloom.gain = 1.0
            extend {
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.draw(circles, rgBa, seconds * 1.0)
                    drawer.draw(crosses, rgBa.opacify(0.8))
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                chromaticAberration.aberrationFactor = cos(seconds / 8.0 * PI).pow(1024) * 8.0
                chromaticAberration.apply(blurred, blurred)
                drawer.image(blurred)
            }
        }
    }
}