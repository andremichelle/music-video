package draw

import audio.AudioTransform
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import kotlin.math.exp

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
        val smoothingCoeff = exp(-1.0 / (transform.fps * 0.3)).toFloat() // 300ms release-time
        for (c in channelIndex until numCols) {
            val energyNorm = values[c]
            if (history[c] < energyNorm) {
                history[c] = energyNorm
            } else if (history[c] >= 0f) {
                history[c] = energyNorm + smoothingCoeff * (history[c] - energyNorm)
            }
            var peak = true
            val x = c * (blockWidth + blockPadding)
            for (r in channelIndex until numRows) {
                val threshold = 1f - r / (numRows - 1).toFloat()
                if (peak && threshold < history[c]) {
                    peak = false
                    drawer.fill = ColorRGBa.WHITE
                } else if (threshold <= energyNorm) {
                    drawer.fill = ColorRGBa.WHITE.opacify(0.60)
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