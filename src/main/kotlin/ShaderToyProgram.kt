import draw.ShaderToy
import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        configure {
            width = 720
            height = 720
            title = "Shadertoy"
        }
        program {
            val renderVideo = false
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/moog.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 8.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val shaderToy = ShaderToy.fromFile("data/shader/moog.fs")
            extend {
                shaderToy.render(window.size * window.scale, seconds * 0.125, uniforms = { shader ->
                })
            }
        }
    }
}