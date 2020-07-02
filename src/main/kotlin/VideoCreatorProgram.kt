import audio.*
import draw.FpsMeter
import draw.FpsMeter.Companion.draw
import net.TrackApi
import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import scene.ImpossibleMission
import java.io.File
import java.nio.file.Paths

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
    val audioPlaybackMode = true
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

            val scene: Scene = Scene.list[3]

            val wavPath = "/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/${scene.trackKey}.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val track = TrackApi.fetch(scene.trackKey).track

            val fpsMeter = FpsMeter()

            val contentScale = if (videoCaptureMode) {
                // call this in terminal to mux audio into video
                println(
                    "ffmpeg -i ${Paths.get("tmp/${scene.trackKey}.mp4").toAbsolutePath()} -i ${wavFile.toPath()
                        .toAbsolutePath()} -c copy tmp/${scene.trackKey}.mkv"
                )
                Vector2(extend(ScreenRecorder()) {
                    outputFile = "tmp/${scene.trackKey}.mp4"
                    quitAfterMaximum = true
                    maximumDuration = wavFormat.seconds()
                    frameRate = 60
                    contentScale = 2.0
                }.contentScale)
            } else {
                window.scale
            }

            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode && !videoCaptureMode) {
                    AudioPlaybackImpl(wavPath)
                } else {
                    AudioPlaybackNone(this)
                }

            val renderer = ImpossibleMission(
                width,
                height,
                contentScale,
                scene.seed,
                wavFormat.seconds(),
                scene.backgroundAlpha,
                AudioTransform(wavFormat, 1024),
                TempoEvaluator(TempoEvent.fetch(scene.trackKey), track.bpm)
            )
            renderer.cover = loadImage(track.cover())
            renderer.shadertoy = scene.shadertoy
            renderer.header = track.name
            renderer.subline = track.authors()

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