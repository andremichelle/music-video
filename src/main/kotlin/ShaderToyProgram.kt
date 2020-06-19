import draw.ShaderToy
import org.openrndr.application

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val shaderToy = ShaderToy.fromFile("data/shader/uplifting.fs")

            extend {
                shaderToy.render(window.size, seconds)
            }
        }
    }
}