package draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.math.mod
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.*
import kotlin.random.Random

class Hud {
    open class Element {
        var x = 0.0
        var y = 0.0

        open fun move(x: Double, y: Double): Element {
            this.x = x
            this.y = y
            return this
        }
    }

    class Circle(random: Random, n: Int, r0: Double, r1: Double) : Element() {
        companion object {
            fun Drawer.draw(circles: List<Circle>, rgBa: ColorRGBa, time: Double) {
                for (circle in circles) {
                    this.draw(circle, rgBa, time)
                }
            }

            fun Drawer.draw(circle: Circle, rgBa: ColorRGBa, time: Double) {
                this.stroke = rgBa
                this.fill = null
                this.lineCap = LineCap.BUTT
                this.lineJoin = LineJoin.MITER
                this.pushTransforms()
                this.translate(circle.x, circle.y)
                val subTime = time * 0.125
                var index = (subTime).toInt() * 3
                for (section in circle.sections) {
                    section.draw(
                        this, mappings[++index % mappings.size]
                            .invoke(if ((index and 1) == 1) subTime else 1.0 - subTime) * 360.0
                    )
                }
                this.popTransforms()
            }

            private val mappings: List<(Double) -> Double> = listOf(
                { x -> x - floor(x) }, // linear
                accelerateAndStop(4.0),
                accelerateAndStop(2.0),
                { x -> abs(sin(x * PI)) },
                { x -> 1.0 - min(1.0, 2.0 * (2.0 * x - floor(2.0 * x))) },
                stopAndGo(4),
                stopAndGo(8),
                stopAndGo(16)
            )

            private fun accelerateAndStop(exp2: Double) = { x: Double -> ((x - floor(x))).pow(exp2) }

            private fun stopAndGo(l: Int): (Double) -> Double {
                val w = { x: Double -> max(2.0 * x - 1.0, 0.0) }
                return { x -> (w.invoke(mod(x * l, 1.0)) + floor(x * l)) / l }
            }
        }

        override fun move(x: Double, y: Double): Circle {
            return super.move(x, y) as Circle
        }

        private val sections: List<Section> = List(n) { index ->
            val s = (r1 - r0) / n.toDouble()
            Section.create(random, r0 + index * s, r0 + (index + 1) * s)
        }

        class Section(
            numSections: Int,
            lengthRatio: Double,
            widthRatio: Double,
            arcRatio: Double,
            startAngle: Double = 0.0,
            r0: Double,
            r1: Double
        ) {
            companion object {
                fun create(random: Random, r0: Double, r1: Double): Section {
                    val numSections = 1 shl (random.nextInt(5) + 1)
                    val lengthRatio =
                        if (random.nextBoolean()) {
                            2.0.pow(-1 - random.nextInt(4).toDouble())
                        } else {
                            0.75
                        }
                    val widthRatio = 2.0.pow(-random.nextInt(random.nextInt(2) + 1).toDouble())
                    val arcRatio = if (random.nextInt(3) == 0) {
                        // create some nice rational number
                        val d = random.nextInt(4) + 1
                        val n = random.nextInt(d) + 1
                        n / d.toDouble()
                    } else {
                        1.0
                    }
                    val startAngle = random.nextInt(8) * 0.125
                    return Section(numSections, lengthRatio, widthRatio, arcRatio, startAngle, r0, r1)
                }
            }

            private val shape: Shape
            private val strokeWeight: Double =
                (r1 - r0) * widthRatio * 0.5 // odd behaviour. they do not touch, when widthRatio is 1

            init {
                val radius: Double = (r0 + r1) * 0.5
                val circle = Circle(0.0, 0.0, radius)
                val nSec = arcRatio / numSections.toDouble()
                val arcLength: Double = lengthRatio * nSec
                val contours: List<ShapeContour> = List(numSections) { index ->
                    val u0 = startAngle + index * nSec
                    circle.contour.sub(u0, u0 + arcLength)
                }
                shape = Shape(contours)
            }

            fun draw(drawer: Drawer, rotation: Double) {
                drawer.pushTransforms()
                drawer.strokeWeight = strokeWeight
                drawer.rotate(rotation)
                drawer.shape(shape)
                drawer.popTransforms()
            }
        }
    }
}