import audio.*
import draw.*
import draw.FpsMeter.Companion.draw
import draw.Hud.Circle.Companion.draw
import net.TrackApi
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.mod
import org.openrndr.shape.Rectangle
import java.io.File
import java.nio.file.Paths
import kotlin.math.*
import kotlin.random.Random

// Try
// https://www.shadertoy.com/view/ls3Xzf (glitch)
// https://github.com/openrndr/openrndr/blob/master/openrndr-openal/src/main/kotlin/AudioPlayer.kt#L23 (Shutdown hook)
// Check out the layer feature https://guide.openrndr.org/#/10_OPENRNDR_Extras/C11_Poisson_fills

// Todo
// How to draw a spectrum with one shader call (send height as uniform?)
// Add cover

@Suppress("ConstantConditionIf")
fun main() {
    val audioPlaybackMode = false
    val videoCaptureMode = false

    application {
        configure {
            width = 960
            height = 540
            title = "Video Preview"
        }
        program {
            extend(Screenshots()) {
                folder = "tmp/"
                scale = 2.0
            }

            val trackKey = "volution"

            val wavPath = "/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/$trackKey.wav"
            val wavFile = File(wavPath)
            val wavFormat = WavFormat.decode(wavFile.readBytes())
            val track = TrackApi.fetch(trackKey).track

            val fpsMeter = FpsMeter()
            val font = org.openrndr.draw.loadFont("data/fonts/IBMPlexMono-Regular.ttf", 18.0)
            val atl = loadImage("data/images/audiotool.png")
            val sBg = loadImage("data/images/hud-frame-spectrum.png")
            val wBg = loadImage("data/images/hud-frame-waveform.png")

            if (videoCaptureMode) {
                // call this in terminal to mux audio into video
                println(
                    "ffmpeg -i ${Paths.get("tmp/movie.mp4").toAbsolutePath()} -i ${wavFile.toPath()
                        .toAbsolutePath()} -c copy tmp/movie.mkv"
                )
                extend(ScreenRecorder()) {
                    outputFile = "tmp/$trackKey.mp4"
                    quitAfterMaximum = true
                    maximumDuration = wavFormat.seconds()
                    frameRate = 60
                    contentScale = 2.0
                }
            }

            val audioPlayback: AudioPlayback =
                if (audioPlaybackMode && !videoCaptureMode) {
                    AudioPlaybackImpl(wavPath)
                } else {
                    AudioPlaybackNone(this)
                }
            val transform = AudioTransform(wavFormat)

            // Spectra
            val s0 = createSpectrum(sBg)
            val s1 = createSpectrum(sBg)
            val cx = width / 2 - 170
            val cy = height - 320 + 128
            s0.move(cx, cy).reflect()
            s1.move(cx + 208, cy)

            // Waveforms
            val w0: Waveform = createWaveform(wBg)
                .move(s0.x - 3, cy + 104)
            val w1: Waveform = createWaveform(wBg)
                .move(s1.x - 3, cy + 104)
                .reflect()

            val shaderToy = ShaderToy.fromFile("data/shader/showmaster.fs")

            val random = Random(0x306709)
            val rgBa = ColorRGBa.fromHex(0x41F8FF)

            val circles: List<Hud.Circle> = listOf(
                Hud.Circle(random, 5, 4.0, 24.0)
                    .move(cx + 169, cy + 14),
                Hud.Circle(random, 5, 4.0, 24.0)
                    .move(cx + 169, cy + 70),
                Hud.Circle(random, 5, 4.0, 24.0)
                    .move(cx + 169, cy + 126)
            )

            val rt = renderTarget(width, height) {
                colorBuffer()
                depthBuffer()
            }
            val blurred = colorBuffer(width, height)
            val bloom = GaussianBloom()
            bloom.window = 25
            bloom.sigma = 1.0
            bloom.gain = 2.0
            val chromaticAberration = ChromaticAberration()

            val normDb = normDb()

            if (!videoCaptureMode) {
                audioPlayback.play()
            }
            extend {
                val playBackSeconds = if (videoCaptureMode) seconds else audioPlayback.seconds()
                val bars = secondsToBars(playBackSeconds, track.bpm)
                transform.advance(playBackSeconds)
                shaderToy.render(window.size * window.scale, playBackSeconds) { shader ->
                    val value = normDb(transform.peakDb())
                    shader.uniform("iPeak", 0.125 + value.pow(16.0) * 0.25)
                }
                drawer.image(atl, (width - atl.width * 0.125) - 8.0, 8.0, atl.width * 0.125, atl.height * 0.125)
                drawer.isolatedWithTarget(rt) {
                    drawer.stroke = null
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    s0.draw(drawer, rgBa, transform, 0)
                    s1.draw(drawer, rgBa, transform, 1)

                    drawer.fill = rgBa
                    w0.render(drawer, transform.channel(0))
                    w1.render(drawer, transform.channel(1))

                    drawer.draw(circles, rgBa, bars * 2.0)
                }
                bloom.apply(rt.colorBuffer(0), blurred)
                val timedInterval = max(ceil(1.0 - mod(bars, 8.0)), 0.0)
                chromaticAberration.aberrationFactor =
                    (1.0 + cos(bars * PI * 2.0)) +
                            cos(bars * PI * 0.5).pow(32.0) * 8.0 * timedInterval
                chromaticAberration.apply(blurred, blurred)
                drawer.image(blurred)

                drawer.fill = rgBa.opacify(0.3)
                drawer.stroke = null
                val widthL = normDb(transform.peakDb(0)) * 172.0
                val widthR = normDb(transform.peakDb(1)) * 172.0
                drawer.rectangle(width / 2.0 + 4, height - 32.0, widthR, 8.0)
                drawer.rectangle(width / 2.0 - widthL - 4, height - 32.0, widthL, 8.0)

                drawer.fontMap = font
                drawer.fill = ColorRGBa.WHITE
                drawer.text(track.user.name, 8.0, 16.0)
                drawer.text(track.name, 8.0, 32.0)

                val fadeOutTime = 5.0
                val total = wavFormat.seconds()
                val alphaInv = clamp((playBackSeconds - (total - fadeOutTime)) / fadeOutTime, 0.0, 1.0)
                drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
                drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))

                if (!videoCaptureMode) {
                    drawer.draw(fpsMeter, seconds)
                }
            }
        }
    }
}

private fun createWaveform(wBg: ColorBuffer) = Waveform(135.0, 36.0)
    .background(wBg, Vector2(-9.0, -10.0), 1.0)

private fun createSpectrum(sBg: ColorBuffer) = Spectrum(26, 21, 4, 2, 1)
    .background(sBg, Vector2(-8.0, -18.0), 1.0)