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
            val renderVideo = true
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/booting.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 16.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val shaderToy = ShaderToy.fromFile("data/shader/booting.fs") {}
            extend {
                shaderToy.render(window.size * window.scale, seconds, 120.0)
            }
        }
    }
}