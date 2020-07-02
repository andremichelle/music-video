import audio.AudioPlayback
import audio.AudioPlaybackSample
import audio.AudioPlaybackNone
import draw.FpsMeter
import draw.FpsMeter.Companion.draw
import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import scene.ImpossibleMission
import scene.SceneRenderer

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)
// https://github.com/openrndr/openrndr/blob/master/openrndr-openal/src/main/kotlin/AudioPlayer.kt#L23 (Shutdown hook)
// Check out the layer feature https://guide.openrndr.org/#/10_OPENRNDR_Extras/C11_Poisson_fills
// Render hud-frame as SVG (not working out of the box, Illustrator export?)

// Todo
// How to draw a spectrum with one shader call https://www.shadertoy.com/view/WtlyDj
// Allow to place shadertoy arbitrary on stage
// flying hexagons
// special chars not rendered

@Suppress("ConstantConditionIf")
fun main() {
    val audioPlaybackMode = false
    val videoCaptureMode = false

    application {
        configure {
            width = 960
            height = 540
            title = if (videoCaptureMode) "Video recording" else "Video Preview"
        }
        program {
            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }

            val scene: Scene

            scene = MixScene.list[0]
//            scene = TrackScene.list[0]

            val fpsMeter = FpsMeter()
            val contentScale = if (videoCaptureMode) {
                scene.printMuxCommand()
                Vector2(extend(ScreenRecorder()) {
                    outputFile = scene.mp4OutputPath()
                    quitAfterMaximum = true
                    maximumDuration = scene.duration()
                    frameRate = 60
                    contentScale = 2.0
                }.contentScale)
            } else {
                window.scale
            }
            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode && !videoCaptureMode) {
                    AudioPlaybackSample(scene.wavPath())
                } else {
                    AudioPlaybackNone(this)
                }

            @Suppress("USELESS_IS_CHECK") val renderer: SceneRenderer = when (scene) {
                is MixScene -> ImpossibleMission.fromMixScene(scene, width, height, contentScale)
                is TrackScene -> ImpossibleMission.fromTrackScene(scene, width, height, contentScale)
                else -> throw IllegalStateException()
            }

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            extend {
                renderer.render(this, if (videoCaptureMode) seconds else audioPlayback.seconds())
                if (!videoCaptureMode) {
                    drawer.draw(fpsMeter, seconds)
                }
            }
        }
    }
}