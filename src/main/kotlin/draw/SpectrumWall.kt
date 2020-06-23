package draw

import audio.AudioTransform
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
    private var reflect = false

    fun reflect(value: Boolean = true): SpectrumWall {
        reflect = value
        return this
    }

    fun draw(drawer: Drawer, transform: AudioTransform, channelIndex: Int) {
        transform.mapSpectrum(values, channelIndex)
        if (reflect) {
            values.reverse()
        }
        for (c in channelIndex until numCols) {
            val energyNorm = values[c]
            if (history[c] < energyNorm) {
                history[c] = energyNorm
            } else if (history[c] >= 0f) {
                history[c] -= 0.01f
            }
            var peak = true
            val x = c * (blockWidth + blockPadding)
            for (r in channelIndex until numRows) {
                val threshold = 1f - r / (numRows - 1).toFloat()
                if (peak && threshold < history[c]) {
                    peak = false
                    drawer.fill = ColorRGBa.BLACK
                } else if (threshold <= energyNorm) {
                    drawer.fill = ColorRGBa.WHITE
                } else {
                    drawer.fill = ColorRGBa.WHITE.opacify(0.08)
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

    @Suppress("unused")
    fun width(): Int {
        return numCols * blockWidth + (numCols - 1) * blockPadding
    }

    @Suppress("unused")
    fun height(): Int {
        return numRows * blockHeight + (numRows - 1) * blockPadding
    }
}