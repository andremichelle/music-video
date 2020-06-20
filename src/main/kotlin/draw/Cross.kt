package draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.shape.ContourBuilder
import org.openrndr.shape.ShapeContour

class Cross(radius: Double) : Hud.Element() {
    private val contours: List<ShapeContour>

    init {
        val builder = ContourBuilder(true)
        builder.moveTo(-radius - 1, 0.0)
        builder.lineTo(radius, 0.0)
        builder.moveTo(0.0, -radius - 1)
        builder.lineTo(0.0, radius)
        contours = builder.result
    }

    companion object {
        fun Drawer.draw(crosses: List<Cross>, rgBa: ColorRGBa) {
            this.stroke = rgBa
            this.strokeWeight = 1.0
            this.lineCap = LineCap.BUTT
            this.lineJoin = LineJoin.MITER
            this.fill = null
            for (cross in crosses) {
                this.pushTransforms()
                this.translate(cross.x, cross.y)
                this.contours(cross.contours)
                this.popTransforms()
            }
        }
    }

    override fun move(x: Double, y: Double): Cross {
        return super.move(x, y) as Cross
    }
}