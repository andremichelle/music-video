import draw.ShaderToy
import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        configure {
            width = 1280
            height = 720
            title = "Shadertoy"
        }
        program {
            val renderVideo = false
            @Suppress("ConstantConditionIf")
            if (renderVideo) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/movie.mp4"
                    quitAfterMaximum = true
                    maximumDuration = 10.0
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")
            extend {
                shaderToy.render(window.size * window.scale, seconds * 0.25)
            }
        }
    }
}