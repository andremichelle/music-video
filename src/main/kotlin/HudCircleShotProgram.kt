import draw.Hud
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 768
            height = 768
            title = "Hud"
        }
        program {
            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }
            val random = Random(0x30312069)
            val rgBa = ColorRGBa.fromHex(0xFFFFFF)

            val circle = Hud.Circle(random, 10, 8.0, 376.0)
                .move(384, 384)

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
                    drawer.draw(circle, rgBa, seconds)
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                chromaticAberration.aberrationFactor = cos(seconds / 8.0 * PI).pow(1024) * 8.0
                chromaticAberration.apply(blurred, blurred)
                drawer.image(blurred)
            }
        }
    }
}