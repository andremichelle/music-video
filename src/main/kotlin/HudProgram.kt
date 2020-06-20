import draw.Cross
import draw.Cross.Companion.draw
import draw.Hud
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 752
            height = 752
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

            val random = Random(0xBB909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)

            val circles: List<List<Hud.Circle>> = List(2) {
                val n = 6 + random.nextInt(11)
                List(25) { index ->
                    val c = index % 5
                    val r = index / 5
                    val x = 88.0 + c * (128 + 16)
                    val y = 88.0 + r * (128 + 16)
                    Hud.Circle(random, n, 8.0, 64.0)
                        .move(x, y)
                }
            }

            val crosses: List<Cross> = List(36) { index ->
                val c = index % 6
                val r = index / 6
                val x = 16.0 + c * (128 + 16)
                val y = 16.0 + r * (128 + 16)
                Cross(2.0).move(x, y)
            }

            val opacityStep = 1
            extend {
                for ((index, list) in circles.withIndex()) {
                    val opacity = (index + opacityStep) / (circles.size + opacityStep).toDouble()
                    drawer.draw(list, rgBa.opacify(opacity), seconds * 1.0)
                }
                drawer.draw(crosses, rgBa.opacify(0.5))
            }
        }
    }
}