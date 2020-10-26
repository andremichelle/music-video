import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.contour
import java.nio.file.Paths
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val numFrame = 120 * 4
            if (true) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/loop.mp4"
                    quitAfterMaximum = true
                    maximumFrames = numFrame.toLong() - 1
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            println(
                "ffmpeg -i ${
                    Paths.get("tmp/loop.mp4")
                    .toAbsolutePath()} -i data/loops/haus.wav -c copy tmp/loop.mkv"
            )

            extend {
                val phase = (frameCount.toDouble() / numFrame.toDouble()) * PI * 2.0

                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.stroke = ColorRGBa.PINK
                drawer.translate(width * 0.5, height * 0.5)
                drawer.contour(contour {
                    moveTo(0.0, 0.0)
                    lineTo(sin(phase) * 256.0, -cos(phase) * 256.0)
                })
            }

            window.closed.listen {
            }
        }
    }
}