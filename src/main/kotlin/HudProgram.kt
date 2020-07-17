import draw.Cross
import draw.Cross.Companion.draw
import draw.Hud
import draw.Hud.Circle.Companion.draw3D
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector3
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

            val random = Random(0x90808909)
            val rgBa = ColorRGBa.WHITE//ColorRGBa.fromHex(0x41F8FF)

            val numCircles = 1
            val circle3D: List<Hud.Circle> = List(numCircles) {
                Hud.Circle(random, 16, 0.0, 128.0)
            }

            var ax = 0.0
            var ay = 0.0
            var tx: Double
            var ty: Double

            extend {
                val ranAngle = Random(0xFFF + (seconds * 0.5).toInt())
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.pushTransforms()
                drawer.perspective(90.0, width.toDouble() / height, 0.01, 2000.0)
                drawer.depthWrite = true
                drawer.lookAt(Vector3(0.0, 0.0, -256.0), Vector3.ZERO, Vector3.UNIT_Y)
                tx = ranAngle.nextDouble(-45.0, 45.0)
                ty = ranAngle.nextDouble(-45.0, 45.0)
                ax += (tx - ax) * 0.04
                ay += (ty - ay) * 0.04
                drawer.rotate(Vector3.UNIT_X, ax)
                drawer.rotate(Vector3.UNIT_Y, ay)
//                drawer.translate(0.0, 0.0, 256.0)
                for (index in circle3D.withIndex()) {
                    drawer.translate(0.0, 0.0, 0.0)
                    drawer.draw3D(index.value, rgBa, seconds)
                }
                drawer.draw(listOf(Cross(64.0)), rgBa)
                drawer.popTransforms()
            }
        }
    }
}