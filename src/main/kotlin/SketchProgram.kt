import audio.*
import draw.BezierSpline
import draw.Estimation
import draw.FPSMeter
import draw.FPSMeter.Companion.draw
import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector3
import org.openrndr.shape.contour
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val trackKey = "jsemznbzvodh"
            val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav")
            val audioFormat = WavStream.forFile(wavPath.toFile())
            val duration = audioFormat.seconds()
            if (true) {
                extend(ScreenRecorder()) {
                    outputFile = "tmp/$trackKey.mp4"
                    quitAfterMaximum = true
                    maximumDuration = duration
                    frameRate = 30
                    contentScale = 2.0
                }
            }
            val playback =
                if (false)
                    AudioPlaybackStream.create(WavStream.forFile(wavPath.toFile()))
                else AudioPlaybackNone(this)
            val transform = AudioTransform(audioFormat, 4096, 60.0, 20000.0, -96.0, -9.0, 60.0)

            println(
                "ffmpeg -i ${Paths.get("tmp/${trackKey}.mp4")
                    .toAbsolutePath()} -i ${wavPath.toAbsolutePath()} -c copy tmp/${trackKey}.mkv"
            )

            val numPoints = 512
            val numCurves = 8192
            val zRange = 1024.0
            val curves = List(numCurves) { FloatArray(numPoints) }
            val valid = BooleanArray(numCurves)

            val xMarch = 2.0 / numPoints
            val xScale = width / 2.0
            val yScale = height / 2.0
            val spline = BezierSpline(numPoints)
            val meter = FPSMeter()

            playback.play()

            var writeIndex = 0

            var cx = 0.0
            var cy = 0.0
            var cz = -2048.0
            var tx: Double
            var ty: Double
            var tz: Double

            val estimate = Estimation(window)

            extend {
                val time = playback.seconds()
                val bars = secondsToBars(time, 120.0)
                transform.advance(time)
                estimate.update(time / duration)

                val random = Random((bars * 0.25).toInt())
                tx = random.nextDouble(-512.0, 512.0)
                ty = random.nextDouble(128.0, 512.0)
                tz = random.nextDouble(-512.0, -256.0)

                cx += (tx - cx) * 0.02
                cy += (ty - cy) * 0.02
                cz += (tz - cz) * 0.02

                transform.mapSpectrum(curves[writeIndex], 0)
                valid[writeIndex] = true
                drawer.clear(ColorRGBa.TRANSPARENT)
                drawer.pushTransforms()
                drawer.perspective(75.0, width.toDouble() / height, 0.01, 20000.0)
                drawer.lookAt(Vector3(cx, cy, cz), Vector3.ZERO, Vector3.UNIT_Y)

                for (index in 0.until(numCurves)) {
                    val offset = index / numCurves.toDouble()
                    val curveIndex = (writeIndex + index + 1) % numCurves
                    if (!valid[curveIndex]) {
                        continue
                    }
                    val curve = curves[curveIndex]
                    drawer.stroke = ColorHSLa(
                        fract(offset + curveIndex / (numCurves - 1.0)) * 360.0,
                        0.7,
                        0.7,
                        offset.pow(0.125) / 64.0
                    ).toRGBa()
//                    drawer.stroke = ColorRGBa.WHITE.opacify(offset.pow(0.125) / 128.0)
                    drawer.strokeWeight = 1.75
                    drawer.pushTransforms()
                    drawer.translate(0.0, 0.0, (offset - 0.5) * zRange)
                    if (false) {
                        for (i in 0.until(numPoints)) {
                            spline.set(i, (-1.0 + i * xMarch) * xScale, curve[i] * yScale)
                        }
                        drawer.contour(spline.create())
                    } else {
                        drawer.contour(contour {
                            moveTo(-1.0 * xScale, curve[0] * yScale)
                            for (i in 1.until(numPoints)) {
                                lineTo((-1.0 + i * xMarch) * xScale, curve[i] * yScale)
                            }
                        })
                    }
                    drawer.popTransforms()
                }

                drawer.popTransforms()
//                drawer.draw(meter, seconds)

                writeIndex = (writeIndex + 1) % numCurves
                for (i in 0..32) {
                    transform.mapSpectrum(curves[writeIndex], 0)
                    writeIndex = (writeIndex + 1) % numCurves
                }
            }

            window.closed.listen {
                playback.stop()
            }
        }
    }
}

fun fract(value: Double) = value - floor(value)