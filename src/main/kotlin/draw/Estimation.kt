package draw

import audio.formatDuration
import org.openrndr.Program

class Estimation(private val window: Program.Window) {
    var lastPercent = 0
    var startTime = System.nanoTime()

    fun update(progress: Double) {
        val percent = (progress * 100000.0).toInt()
        if (lastPercent != percent) {
            val computationTime = (System.nanoTime() - startTime) / 1000000000.0
            val remaining = (computationTime / progress) - computationTime
            val message = "Video recording (${(percent / 1000.0).toInt()}%" +
                    " remaining: ${formatDuration(remaining.toInt())})"
            window.title = message
            lastPercent = percent
        }
    }
}