import audio.AudioPlayback
import audio.AudioPlaybackNone
import audio.AudioPlaybackStream
import draw.Estimation
import draw.FPSMeter
import draw.FPSMeter.Companion.draw
import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import scene.*

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
// Bubbles > https://www.shadertoy.com/view/4dl3zn

@Suppress("ConstantConditionIf")
fun main() {
    val videoCaptureMode = true
    val audioPlaybackMode = !videoCaptureMode

    application {
        configure {
            width = 960
            height = 540
            title = if (videoCaptureMode) "Video recording" else "Video Preview"
        }
        program {
            val sceneSetup: SceneSetup
            sceneSetup = TrackSceneSetup.fwbse4jk2dq
//            sceneSetup = MixSceneSetup.EDM

            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }

            val fpsMeter = FPSMeter()
            val contentScale = if (videoCaptureMode) {
                sceneSetup.printMuxCommand()
                Vector2(extend(ScreenRecorder()) {
                    outputFile = sceneSetup.mp4OutputPath()
                    quitAfterMaximum = true
                    maximumDuration = sceneSetup.duration()
                    frameRate = sceneSetup.fps()
                    contentScale = 2.0
                }.contentScale)
            } else {
                window.scale
            }
            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode && !videoCaptureMode) {
                    AudioPlaybackStream.create(sceneSetup.createAudioFormat())
                } else {
                    AudioPlaybackNone(this)
                }

            @Suppress("USELESS_IS_CHECK") val renderer: SceneRenderer = when (sceneSetup) {
                is MixSceneSetup -> EdmScene.fromMixScene(sceneSetup, width, height, contentScale)
                is TrackSceneSetup -> ImpossibleMission.fromTrackScene(sceneSetup, width, height, contentScale)
                else -> throw IllegalStateException()
            }

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            val estimation = Estimation(window)
            extend {
                renderer.render(this, if (videoCaptureMode) seconds else audioPlayback.seconds())
                if (!videoCaptureMode) {
                    drawer.draw(fpsMeter, seconds)
                }
                estimation.update(seconds / sceneSetup.duration())
            }
            window.closed.listen {
                audioPlayback.stop()
            }
        }
    }
}