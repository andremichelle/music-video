package draw

import org.openrndr.draw.Drawer
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
            fun Drawer.draw(circle: Circle, time: Double) {
                this.pushTransforms()
                this.translate(circle.x, circle.y)
                val subTime = time * 0.125
                var index = (subTime).toInt()
                for (section in circle.sections) {
                    section.draw(
                        this, mappings[++index % mappings.size]
                            .invoke(if ((index and 1) == 1) subTime else 1.0 - subTime) * 180.0
                    )
                }
                this.popTransforms()
            }

            private val mappings: List<(Double) -> Double> = listOf(
                { time -> time - floor(time) },
                { time -> (1.0 - (time - floor(time))).pow(4.0) },
                { time -> (1.0 - (time - floor(time))).pow(2.0) },
                { time -> abs(sin(time * PI)) },
                { time -> 1.0 - min(1.0, 2.0 * (2.0 * time - floor(2.0 * time))) },
                fun(time: Double): Double {
                    val l = 4
                    val w = { x: Double -> max(2 * x - 1.0, 0.0) }
                    return (w.invoke(mod(time * l, 1.0)) + floor(time * l)) / l
                }
            )
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
            r0: Double,
            r1: Double,
            startAngle: Double = 0.0
        ) {
            companion object {
                fun create(random: Random, r0: Double, r1: Double): Section {
                    val numSections = 1 shl (random.nextInt(6) + 1)
                    val lengthRatio =
                        if (random.nextBoolean())
                            2.0.pow(-1 - random.nextInt(5).toDouble())
                        else
                            1.0 - 2.0.pow(-1 - random.nextInt(2).toDouble())
                    val widthRatio = 2.0.pow(-random.nextInt(random.nextInt(2) + 1).toDouble())
                    return Section(numSections, lengthRatio, widthRatio, r0, r1)
                }
            }

            private val shape: Shape
            private val strokeWeight: Double = ceil((r1 - r0) * widthRatio * 0.5)

            init {
                val radius: Double = (r0 + r1) * 0.5
                val circle = Circle(0.0, 0.0, radius)
                val arcLength: Double = lengthRatio / numSections.toDouble()
                val contours: List<ShapeContour> = List(numSections) { index ->
                    val u0 = startAngle + index / numSections.toDouble() - arcLength * 0.5
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