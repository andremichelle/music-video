package draw

import audio.AudioVisualMapping
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer

class SpectrumWall(
    private val numCols: Int,
    private val numRows: Int,
    private val blockWidth: Int,
    private val blockHeight: Int,
    private val blockPadding: Int
) {
    private val values: FloatArray = FloatArray(numCols)
    private val history: FloatArray = FloatArray(numCols)

    fun draw(drawer: Drawer, mapping: AudioVisualMapping) {
        mapping.write(values)
        for (c in 0 until numCols) {
            val energyNorm = values[c]
            if (history[c] < energyNorm) {
                history[c] = energyNorm
            } else if (history[c] >= 0f) {
                history[c] -= 0.01f
            }
            var peak = true
            val x = c * (blockWidth + blockPadding)
            for (r in 0 until numRows) {
                val threshold = 1f - r / (numRows - 1).toFloat()
                if (peak && threshold < history[c]) {
                    peak = false
                    drawer.fill = ColorRGBa.BLACK
                } else if (threshold <= energyNorm) {
                    drawer.fill = ColorRGBa.WHITE
                } else {
                    drawer.fill = ColorRGBa.WHITE.opacify(0.02)
                }
                val y = r * (blockHeight + blockPadding)
                drawer.rectangle(
                    x.toDouble(),
                    y.toDouble(),
                    blockWidth.toDouble(),
                    blockHeight.toDouble()
                )
            }
        }
    }
}