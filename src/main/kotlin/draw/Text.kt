package draw

import java.awt.Canvas
import java.awt.Font

class TextField {
    val canvas: Canvas = Canvas()

    init {
        canvas.setSize(256, 256)
        canvas.graphics.font = Font("Arial", Font.PLAIN, 13)
        canvas.graphics.drawString("ABCDEFGHIJK", 0, 0)
    }
}