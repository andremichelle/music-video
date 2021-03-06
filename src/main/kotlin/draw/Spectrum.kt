package draw

import audio.AudioTransform
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2
import kotlin.math.exp
import kotlin.math.max

class Spectrum(
    private val numCols: Int,
    private val numRows: Int,
    private val blockWidth: Int,
    private val blockHeight: Int,
    private val blockPadding: Int
) : Hud.Element() {
    private val values: FloatArray = FloatArray(numCols)
    private val history: FloatArray = FloatArray(numCols)
    private var reflect = false

    fun reflect(value: Boolean = true): Spectrum {
        reflect = value
        return this
    }

    override fun background(buffer: ColorBuffer, tl: Vector2, scale: Double): Spectrum {
        return super.background(buffer, tl, scale) as Spectrum
    }

    fun draw(
        drawer: Drawer,
        rgBa: ColorRGBa,
        transform: AudioTransform,
        channelIndex: Int
    ) {
        begin(drawer)
        transform.mapSpectrum(values, channelIndex)
        if (reflect) {
            drawer.translate(width().toDouble(), 0.0)
            drawer.scale(-1.0, 1.0)
        }
        val cValue = rgBa.opacify(0.40)
        val smoothingCoeff = exp(-1.0 / (transform.fps * 0.3)).toFloat() // 300ms release-time
        for (c in 0 until numCols) {
            val energyNorm = values[c]
            if (history[c] < energyNorm) {
                history[c] = energyNorm
            } else if (history[c] >= 0f) {
                val target = max(0.0f, energyNorm)
                history[c] = target + smoothingCoeff * (history[c] - target)
            }
            var peak = true
            val x = c * (blockWidth + blockPadding)
            for (r in channelIndex until numRows) {
                val threshold = 1f - r / (numRows - 1).toFloat()
                if (peak && threshold <= history[c]) {
                    peak = false
                    drawer.fill = rgBa
                } else if (threshold <= energyNorm) {
                    drawer.fill = cValue
                } else {
                    continue
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
        end(drawer)
    }

    @Suppress("unused")
    fun width(): Int {
        return numCols * blockWidth + (numCols - 1) * blockPadding
    }

    @Suppress("unused")
    fun height(): Int {
        return numRows * blockHeight + (numRows - 1) * blockPadding
    }

    override fun move(x: Int, y: Int): Spectrum {
        return super.move(x, y) as Spectrum
    }

    init {
        history.fill(0.0f)
    }
}