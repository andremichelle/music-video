package draw

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.floor

@Suppress("MemberVisibilityCanBePrivate")
class Waveform(val width: Double, val height: Double) : Hud.Element() {
    override fun move(x: Int, y: Int): Waveform {
        return super.move(x, y) as Waveform
    }

    override fun background(buffer: ColorBuffer, tl: Vector2, scale: Double): Waveform {
        return super.background(buffer, tl, scale) as Waveform
    }

    private val topValue = 1.0
    private val bottomValue = -1.0

    private var reflect = false

    fun reflect(value: Boolean = true): Waveform {
        reflect = value
        return this
    }

    fun render(drawer: Drawer, raw: FloatArray) {
        begin(drawer)
        if(reflect) {
            raw.reverse()
        }
        val samplesEachPixel = raw.size / width
        val scale: Double = (height - 1.0) / (bottomValue - topValue)
        val rectangles: MutableList<Rectangle> = mutableListOf()
        var from = 0.0
        var indexFrom = from.toInt()
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for (x in 0 until width.toInt()) {
            val to = from + samplesEachPixel
            val indexTo = to.toInt()
            while (indexFrom < indexTo) {
                val value = raw[indexFrom++].toDouble()
                if (min > value) {
                    min = value
                }
                if (max < value) {
                    max = value
                }
            }
            val yMin: Double = floor((min - topValue) * scale)
            val yMax: Double = floor((max - topValue) * scale)
            if (yMin == yMax) {
                rectangles.add(Rectangle(x.toDouble(), yMin, 1.0, 1.0))
            } else {
                rectangles.add(Rectangle(x.toDouble(), yMin, 1.0, yMax - yMin))
            }
            val tmp = max
            max = min
            min = tmp
            from = to
            indexFrom = indexTo
        }
        drawer.rectangles(rectangles)
        end(drawer)
    }
}