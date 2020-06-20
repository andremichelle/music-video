import draw.Hud
import draw.Hud.Circle.Companion.draw
import draw.ShaderToy
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 704
            height = 704
        }
        program {
            val random = Random(0xFFC)

            val circles: List<Hud.Circle> = List(25) { index ->
                val x = index % 5
                val y = index / 5
                Hud.Circle(random, 16, 8.0, 64.0).move(64.0 + x * (128 + 16), 64.0 + y * (128 + 16))
            }

            val shaderToy = ShaderToy.fromFile("data/shader/uplifting.fs")

            extend {
                if (false) {
                    shaderToy.render(window.size, seconds);
                }

                drawer.stroke = ColorRGBa.WHITE
                for (circle in circles) {
                    drawer.draw(circle, ColorRGBa.fromHex(0x0EC5FF), seconds * 2.0)
                }
            }
        }
    }
}