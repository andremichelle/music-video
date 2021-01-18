package draw

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.io.File
import java.net.URL

typealias JShape = java.awt.Shape

object FontShape {
    fun String.getShape(fontName: String, from: Vector2, fontSize: Double = 128.0): Shape {
        val stream = try {
            fontName.asResource()?.openStream()
        } catch (e: NullPointerException) {
            File(fontName).inputStream()
        }
        println(stream)
        val font = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(fontSize.toFloat())
        println(font)
        val context = FontRenderContext(null, true, true)
        val shape = GeneralPath()
        val layout = TextLayout(this, font, context)
        val transform = AffineTransform.getTranslateInstance(from.x, from.y)
        val outline = layout.getOutline(transform)
        shape.append(outline, true)
        return getPoints(shape)
    }

    private fun getPoints(shape: JShape): Shape {
        val iterator: PathIterator = shape.getPathIterator(null)
        val coordinates = DoubleArray(6)
        var x = 0.0
        var y = 0.0
        var cursor = Vector2(x, y)
        val contours = mutableListOf<ShapeContour>()
        val segments = mutableListOf<Segment>()

        while (!iterator.isDone) {
            var segment: Segment? = null
            when (iterator.currentSegment(coordinates)) {
                PathIterator.SEG_CLOSE -> {
                    contours.add(ShapeContour(segments.toList(), true))
                    segments.clear()
                }
                PathIterator.SEG_QUADTO -> {
                    val x1 = coordinates[0]
                    val y1 = coordinates[1]
                    val x2 = coordinates[2]
                    val y2 = coordinates[3]
                    x = x2
                    y = y2
                    segment = Segment(start = cursor, c0 = Vector2(x1, y1), end = Vector2(x2, y2))
                }
                PathIterator.SEG_CUBICTO -> {
                    val x1 = coordinates[0]
                    val y1 = coordinates[1]
                    val x2 = coordinates[2]
                    val y2 = coordinates[3]
                    val x3 = coordinates[4]
                    val y3 = coordinates[5]
                    x = x3
                    y = y3
                    segment = Segment(start = cursor, c0 = Vector2(x1, y1), c1 = Vector2(x2, y2), end = Vector2(x3, y3))
                }
                PathIterator.SEG_LINETO -> {
                    val x1 = coordinates[0]
                    val y1 = coordinates[1]
                    x = x1
                    y = y1
                    segment = Segment(start = cursor, end = Vector2(x1, y1))
                }
                PathIterator.SEG_MOVETO -> {
                    val x1 = coordinates[0]
                    val y1 = coordinates[1]
                    x = x1
                    y = y1
                }
            }
            cursor = Vector2(x, y)
            if (segment != null) {
                segments.add(segment)
            }
            iterator.next()
        }
        return Shape(contours)
    }
}

fun String.asResource(): URL? {
    return object {}.javaClass.classLoader.getResource(this)
}