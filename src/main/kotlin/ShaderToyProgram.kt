import draw.ShaderToy
import org.openrndr.Fullscreen
import org.openrndr.application

fun main() {
    application {
        configure {
            width = 1280
            height = 720
            fullscreen = Fullscreen.SET_DISPLAY_MODE
        }
        program {
            val shaderToy = ShaderToy.fromFile("data/shader/uplifting.fs")

            extend {
                shaderToy.render(window.size, seconds)
            }
        }
    }
}