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
import org.openrndr.ffmpeg.ScreenRecorder
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

            val random = Random(0x303909)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)

            val circles: List<List<Hud.Circle>> = List(2) {
                val n = 6 + random.nextInt(4)
                List(25) { index ->
                    val c = index % 5
                    val r = index / 5
                    val x = 88.0 + c * (128 + 16)
                    val y = 88.0 + r * (128 + 16)
                    Hud.Circle(random, n, 16.0, 64.0)
                        .move(x, y)
                }
            }

            val crosses: List<Cross> = List(25) { index ->
                val c = index % 5
                val r = index / 5
                val x = 88.0 + c * (128 + 16)
                val y = 88.0 + r * (128 + 16)
                Cross(6.0).move(x, y)
            }

            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }

            val opacityStep = 1
            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            bloom.window = 1
            bloom.sigma = 1.0
            bloom.gain = 1.0
            extend {
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    for ((index, list) in circles.withIndex()) {
                        val opacity = (index + opacityStep) / (circles.size + opacityStep).toDouble()
                        drawer.draw(list, rgBa.opacify(opacity), seconds * 1.0)
                    }
                    drawer.draw(crosses, rgBa.opacify(0.8))
                }

                bloom.apply(rt.colorBuffer(0), blurred)
                drawer.image(blurred)
            }
        }
    }
}