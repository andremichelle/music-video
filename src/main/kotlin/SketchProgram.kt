import audio.AudioPlaybackStream
import audio.AudioTransform
import audio.WavStream
import draw.BezierSpline
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val trackKey = "78qeujew8w"
            val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav")
            val playback = AudioPlaybackStream.create(WavStream.forFile(wavPath.toFile()))
            val transform = AudioTransform(WavStream.forFile(wavPath.toFile()), 1024)

            val n = 128
            val values = FloatArray(n)
            val padding = 64
            val xMarch = 2.0 / (n - 1.0)
            val xScale = (width - padding * 2.0) / 2.0
            val yScale = height / 3.0
            val spline = BezierSpline(n)

            playback.play()

            extend {
                transform.advance(playback.seconds())
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.stroke = ColorRGBa.WHITE
                drawer.pushTransforms()
                drawer.translate(width * 0.5, height * 0.5)
                transform.mapSpectrum(values, 0)
                for (i in 0.until(n)) {
                    spline.set(i, (-1.0 + i * xMarch) * xScale, -values[i] * yScale)
                }
                drawer.contour(spline.create())
                drawer.popTransforms()
            }

            window.closed.listen {
                playback.stop()
            }
        }
    }
}