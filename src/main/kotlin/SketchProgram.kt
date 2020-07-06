import draw.FontShape.getShape
import org.openrndr.application
import org.openrndr.math.Vector2

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            drawer.shape("some text".getShape("Dialog", Vector2(width / 4.0, height / 2.0)))
            extend {
            }
        }
    }
}