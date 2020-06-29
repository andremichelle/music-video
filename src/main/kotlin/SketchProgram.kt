import org.openrndr.application
import org.openrndr.color.ColorRGBa
import kotlin.random.Random

// https://github.com/openrndr/orx

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val font = org.openrndr.draw.loadFont("data/fonts/Andale Mono.ttf", 12.0)
            extend {
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.fontMap = font

                val random = Random(frameCount)

                for (y in 0..15) {
                    for (x in 0..4) {
                        drawer.text(
                            random.nextLong(0, 0xFFFFFFFF + 1)
                                .toString(16).padStart(8, '0'), 32.0 + x * 96, 32.0 + y * 16
                        )
                    }
                }
            }
        }
    }
}