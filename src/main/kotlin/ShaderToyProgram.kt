import draw.ShaderToy
import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        configure {
            width = 720
            height = 720
            title = "Shadertoy"
            //fullscreen = Fullscreen.SET_DISPLAY_MODE
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

            val shaderToy = ShaderToy.fromFile("data/shader/uplifting.fs")

            extend {
                val time = (frameCount % 600) / 600.0
                shaderToy.render(window.size, time)
            }
        }
    }
}