package draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont

class FPSMeter {
    private val font = loadFont("data/fonts/IBMPlexMono-Regular.ttf", 11.0)

    private var lastTimeStamp: Double = Double.NaN
    private var current = 0
    private var frameCounter = 0

    companion object {
        fun Drawer.draw(fpsMeter: FPSMeter, seconds: Double) {
            fpsMeter.update(seconds)
            fontMap = fpsMeter.font
            fill = ColorRGBa.WHITE
            text("fps: ${fpsMeter.current}", 4.0, height - 4.0)
        }
    }

    private fun update(seconds: Double) {
        frameCounter++
        if (lastTimeStamp.isNaN()) {
            lastTimeStamp = seconds
        } else if (lastTimeStamp + 1.0 <= seconds) {
            lastTimeStamp = seconds
            current = frameCounter
            frameCounter = 0
        }
    }
}