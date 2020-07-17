package draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.math.mod
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.math.*
import kotlin.random.Random

class Hud {
    companion object {
        fun getTimeMapper(index: Int): (Double) -> Double {
            return Mappings[index % Mappings.size]
        }

        private val Mappings: List<(Double) -> Double> = listOf(
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

    open class Element {
        class Background(val colorBuffer: ColorBuffer, val tl: Vector2, val scale: Double)

        private var background: Background? = null

        var x = 0
        var y = 0

        open fun move(x: Int, y: Int): Element {
            this.x = x
            this.y = y
            return this
        }

        open fun background(buffer: ColorBuffer, tl: Vector2, scale: Double): Element {
            background = Background(buffer, tl, scale)
            return this
        }

        fun begin(drawer: Drawer) {
            drawer.pushTransforms()
            drawer.translate(x.toDouble(), y.toDouble())
            background?.let {
                drawer.image(
                    it.colorBuffer, it.tl.x, it.tl.y,
                    it.colorBuffer.width * it.scale, it.colorBuffer.height * it.scale
                )
            }
        }

        fun end(drawer: Drawer) {
            drawer.popTransforms()
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
                circle.begin(this)
                this.stroke = null
                val subTime = time * 0.125
                var index = (subTime).toInt() * 3
                for (section in circle.sections) {
                    this.fill = rgBa.opacify(section.opacity)
                    this.pushTransforms()
                    this.rotate(
                        getTimeMapper(++index)
                            (if ((index and 1) == 1) subTime else 1.0 - subTime) * 360.0
                    )
                    section.draw(this)
                    this.popTransforms()
                }
                circle.end(this)
            }

            fun Drawer.draw3D(circle: Circle, rgBa: ColorRGBa, time: Double) {
                circle.begin(this)
                this.stroke = null
                val subTime = time * 0.125
                var index = (subTime).toInt() * 3
                for (item in circle.sections.withIndex()) {
                    val section = item.value
                    val alpha = item.index / (circle.sections.size - 1.0)
                    this.translate(0.0, 0.0, alpha * abs(sin(subTime * PI)) * -12.0)
                    this.fill = rgBa
                    this.pushTransforms()
                    this.rotate(
                        getTimeMapper(++index)(if ((index and 1) == 1) subTime else 1.0 - subTime) * 360.0
                    )
                    section.draw(this)
                    this.popTransforms()
                }
                circle.end(this)
            }
        }

        override fun move(x: Int, y: Int): Circle {
            return super.move(x, y) as Circle
        }

        private val sections: List<Section> = List(n) { index ->
            val s = (r0 - r1) / n.toDouble()
            Section.create(random, index, r1 + index * s, r1 + (index + 1) * s)
        }

        class Section(
            numSections: Int,
            lengthRatio: Double,
            widthRatio: Double,
            arcRatio: Double,
            val opacity: Double,
            startAngle: Double = 0.0,
            r0: Double,
            r1: Double
        ) {
            companion object {
                fun create(random: Random, index: Int, r0: Double, r1: Double): Section {
                    val numSections = 1 shl (random.nextInt(5) + 1)
                    val lengthRatio =
                        if (random.nextBoolean()) {
                            2.0.pow(-1 - random.nextInt(4).toDouble())
                        } else {
                            0.75
                        }
                    val widthRatio = 2.0.pow(-random.nextInt(random.nextInt(2) + 1).toDouble())
                    val arcRatio = if (random.nextInt(4) == 0) {
                        // create some nice rational number
                        val d = random.nextInt(4) + 1
                        val n = random.nextInt(d) + 1
                        n / d.toDouble()
                    } else {
                        1.0
                    }
                    val opacity = sqrt(((index % 4) + 1) / 4.0) * 0.8
                    val startAngle = random.nextInt(8) * 0.25
                    return Section(numSections, lengthRatio, widthRatio, arcRatio, opacity, startAngle, r0, r1)
                }

                private fun createArc(r0: Double, r1: Double, a0: Double, a1: Double): ShapeContour {
                    return contour {
                        val cs0 = cos(Math.toRadians(a0))
                        val sn0 = sin(Math.toRadians(a0))
                        val cs1 = cos(Math.toRadians(a1))
                        val sn1 = sin(Math.toRadians(a1))
                        val largeArcFlag = a1 - a0 > 180.0
                        moveTo(cs0 * r0, sn0 * r0)
                        arcTo(r0, r0, 0.0, largeArcFlag = largeArcFlag, sweepFlag = true, tx = cs1 * r0, ty = sn1 * r0)
                        lineTo(cs1 * r1, sn1 * r1)
                        arcTo(r1, r1, 0.0, largeArcFlag = largeArcFlag, sweepFlag = false, tx = cs0 * r1, ty = sn0 * r1)
                        close()
                    }
                }

                private fun conic(a0: Double = 0.0, a1: Double = 360.0, frac: Double = 1.0): ShadeStyle {
                    return Conic(a0, a1, frac)
                }
            }

            private val shape: Shape
            private val conicGradient: ShadeStyle?

            init {
                val radius: Double = (r0 + r1) * 0.5
                val width: Double = max(0.5, (r1 - r0) * 0.5 * widthRatio - 0.5)
                val nSec = arcRatio / numSections.toDouble()
                val arcLength: Double = lengthRatio * nSec
                val contours: List<ShapeContour> = List(numSections) { index ->
                    val u0 = startAngle + index * nSec - arcLength * 0.5
                    createArc(
                        radius - width,
                        radius + width,
                        u0 * 360.0,
                        (u0 + arcLength) * 360.0
                    )
                }
                conicGradient = if (arcLength >= 0.25) conic(
                    startAngle * 360.0,
                    (startAngle + nSec) * 360.0,
                    lengthRatio
                ) else {
                    null
                }
                shape = Shape(contours)
            }

            fun draw(drawer: Drawer) {
                drawer.shadeStyle = conicGradient
                drawer.shape(shape)
            }
        }
    }

    @Description("Conic")
    private class Conic(a0: Double = 0.0, a1: Double = 360.0, frac: Double = 1.0) : ShadeStyle() {
        @DoubleParameter("start angle", 0.0, 360.0, order = 0)
        var a0: Double by Parameter()

        @DoubleParameter("end angle", 0.0, 360.0, order = 1)
        var a1: Double by Parameter()

        @DoubleParameter("frac", 0.0, 1.0, order = 2)
        var frac: Double by Parameter()

        init {
            this.a0 = a0
            this.a1 = a1
            this.frac = frac

            fragmentTransform = """
                float PI = 3.1415926536;
                float TAU = 2.0 * PI;
                float a0 = radians(p_a0);
                float a1 = radians(p_a1);
                float cr = cos(a0);
                float sr = sin(a0);
                float ad = mod(a1 - a0, TAU);
                mat2 rm = mat2(cr, -sr, sr, cr);
                vec2 rp = rm * va_position;
                float ac = atan(-rp.y, -rp.x) + PI;
                ad += max(0.0, sign(-ad)) * TAU;
                float f = ac / ad;  
                vec3 a = vec3(1.0, 0.0, 0.0);
                vec3 b = vec3(0.0, 1.0, 0.0);
                float alpha = fract(f+0.02) / p_frac;
                x_fill = vec4(x_fill.rgb, alpha * x_fill.a);
                """
        }
    }
}