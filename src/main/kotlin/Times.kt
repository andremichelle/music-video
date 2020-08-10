import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.contour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    application {
        configure {
            width = 720
            height = 720
            title = "Shadertoy"
        }
        program {
            val renderVideo = false
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/move.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 16.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val n = 16
            val r = width * 0.4
            val t = 2
            var index = 1

            extend {
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.stroke = ColorRGBa.PINK
                drawer.translate(width * 0.5, height * 0.5)
                drawer.contours(listOf(contour {
                    moveTo(r, 0.0)
                    for (i in 1..n) {
                        val a = i / n.toDouble() * PI * 2.0
                        lineTo(cos(a) * r, sin(a) * r)
                    }
                }, contour {
                    moveTo(r, 0.0)
                    for (i in 1..n) {
                        val a = index / n.toDouble() * PI * 2.0
                        lineTo(cos(a) * r, sin(a) * r)
                        index *= t
                    }
                }))
            }
        }
    }
}