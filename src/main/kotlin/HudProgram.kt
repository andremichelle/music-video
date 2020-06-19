import draw.Hud
import draw.Hud.Circle.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 736
            height = 736
        }
        program {
            val renderVideo = true
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/movie.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 16.0
                    frameRate = 60
                }
            }

            val random = Random(0xBB909)
            val circles: List<Hud.Circle> = List(25) { index ->
                val x = index % 5
                val y = index / 5
                Hud.Circle(random, 6 + random.nextInt(11), 8.0, 64.0)
                    .move(80.0 + x * (128 + 16), 80.0 + y * (128 + 16))
            }

            extend {
                drawer.stroke = ColorRGBa.fromHex(0x0EC5FF)
                for (circle in circles) {
                    drawer.draw(circle, seconds * 2.0)
                }
            }
        }
    }
}