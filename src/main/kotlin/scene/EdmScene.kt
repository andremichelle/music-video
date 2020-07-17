package scene

import audio.AudioTransform
import audio.TempoEvaluator
import audio.formatDuration
import audio.normDb
import draw.Hud
import draw.Hud.Circle.Companion.draw
import draw.Hud.Circle.Companion.draw3D
import draw.ShaderToy
import draw.Spectrum
import draw.Waveform
import net.Playlist
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.math.*
import kotlin.random.Random

class EdmScene(
    private val width: Int,
    private val height: Int,
    private val contentScale: Vector2,
    seed: Int,
    private val duration: Double,
    private val backgroundAlpha: Double = 0.6,
    private val transform: AudioTransform,
    private val tempoEvaluator: TempoEvaluator,
    private val onEnterFrame: (EdmScene, Double) -> Unit
) : SceneRenderer() {
    companion object {
        fun fromMixScene(scene: MixSceneSetup, width: Int, height: Int, contentScale: Vector2): SceneRenderer {
            val playlist = scene.playlist()
            val tracks = playlist.tracks
            val updateTrack = { renderer: EdmScene, track: Playlist.Track ->
                renderer.shadertoy = scene.shadertoy
                renderer.header = track.name
                renderer.subline = track.authors()
            }
            var trackIndex = 0
            val renderer = EdmScene(
                width,
                height,
                contentScale,
                scene.seed,
                scene.duration(),
                scene.backgroundAlpha,
                scene.createAudioTransform(),
                TempoEvaluator(emptyList(), playlist.bpm)
            ) { renderer: EdmScene, seconds: Double ->
                if (trackIndex + 1 < tracks.size) {
                    if (seconds >= tracks[trackIndex + 1].position) {
                        trackIndex++
                        updateTrack(renderer, tracks[trackIndex])
                    }
                }
            }
            updateTrack(renderer, tracks[0])
            return renderer
        }
    }

    var shadertoy: ShaderToy? = null
    var header: String? = null
    var subline: String? = null

    private val fontTrack = loadFont("data/fonts/IBMPlexMono-Regular.ttf", 18.0)
    private val fontHex = loadFont("data/fonts/Andale Mono.ttf", 7.0)
    private val fontTimeCode = loadFont("data/fonts/Andale Mono.ttf", 11.0)
    private val frame = loadImage("data/images/hud-frame.png")
    private val txt = loadImage("data/images/txt.png")
    private val atl = loadImage("data/images/audiotool.png")
    private val hero = loadImage("data/images/hero.png")
    private val heroColorMatrix = Matrix55(
        1.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.5, 0.0,
        0.0, 0.0, 0.0, 0.0, 1.0
    )

    private val s0 = createSpectrum()
    private val s1 = createSpectrum()
    private val w0: Waveform = createWaveform()
        .move(216, 464)
    private val w1: Waveform = createWaveform()
        .move(424, 464)
        .reflect()

    private val random = Random(seed)
    private val rgBa = ColorRGBa.fromHex(0xFFFFFF)
    private val circles: List<Hud.Circle> = listOf(
        Hud.Circle(random, 5, 0.0, 24.0).move(384, 408),
        Hud.Circle(random, 6, 4.0, 24.0).move(384, 472),
        Hud.Circle(random, 6, 4.0, 24.0).move(640, 392),
        Hud.Circle(random, 5, 4.0, 16.0).move(640, 440),
        Hud.Circle(random, 6, 4.0, 24.0).move(640, 488),
        Hud.Circle(random, 6, 20.0, 40.0).move(884, 468)
    )
    private val rt = renderTarget(width, height) {
        colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        depthBuffer()
    }
    private val rt3D = renderTarget(128, 128) {
        colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        depthBuffer()
    }
    private val blurred = colorBuffer(width, height)
    private val bloom = GaussianBloom()
    private val chromaticAberration = ChromaticAberration()
    private val normDb = normDb()
    private val circle3D: List<Hud.Circle> = List(1) {
        Hud.Circle(random, 16, 0.0, 128.0)
    }
    private var ax = 0.0
    private var ay = 0.0
    private var tx = 0.0
    private var ty = 0.0

    init {
        s0.move(216, 376).reflect()
        s1.move(424, 376)

        bloom.window = 25
        bloom.sigma = 0.5
        bloom.gain = 1.0
    }

    override fun render(program: Program, seconds: Double) {
        onEnterFrame(this, seconds)

        val drawer = program.drawer
        drawer.clear(ColorRGBa.BLACK)
        transform.advance(seconds)
        tempoEvaluator.advance(seconds)

        shadertoy?.render(program.window.size * contentScale, seconds, tempoEvaluator.bpm())

        val bars = tempoEvaluator.bars()
        drawer.image(atl, (width - atl.width * 0.125) - 8.0, 8.0, atl.width * 0.125, atl.height * 0.125)
        drawer.isolatedWithTarget(rt3D) {
            drawer.clear(ColorRGBa.TRANSPARENT)
            val ranAngle = Random(0xFFF + (bars * 0.5).toInt())
            drawer.pushTransforms()
            drawer.perspective(120.0, width.toDouble() / height, 0.01, 2000.0)
            drawer.depthWrite = true
            drawer.lookAt(Vector3(0.0, 0.0, -80.0), Vector3.ZERO, Vector3.UNIT_Y)
            tx = ranAngle.nextDouble(-45.0, 45.0)
            ty = ranAngle.nextDouble(-45.0, 45.0)
            ax += (tx - ax) * 0.04
            ay += (ty - ay) * 0.04
            drawer.rotate(Vector3.UNIT_X, ax)
            drawer.rotate(Vector3.UNIT_Y, ay)
            for (circle in circle3D) {
                drawer.draw3D(circle, rgBa, bars)
            }
            drawer.popTransforms()
        }
        drawer.isolatedWithTarget(rt) {
            drawer.stroke = null
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.image(frame, 0.0, 0.0, width.toDouble(), height.toDouble())
            s0.draw(drawer, rgBa, transform, 0)
            s1.draw(drawer, rgBa, transform, 1)
            drawer.fill = rgBa
            w0.render(drawer, transform.channel(0))
            w1.render(drawer, transform.channel(1))
            drawer.draw(circles, rgBa, bars * 2.0)
            drawer.fontMap = fontTrack
            drawer.fill = ColorRGBa.WHITE
            header?.let { drawer.text(it, 40.0, 332.0) }
            subline?.let { drawer.text(it, 40.0, 332.0 + 16.0) }
            drawer.fontMap = fontHex
            drawer.fill = ColorRGBa.WHITE
            val r = Random(floor(bars * 16.0).toInt())
            for (y in 0..12) {
                for (x in 0..2) {
                    drawer.text(
                        r.nextInt(0, 0x10000)
                            .toString(16)
                            .toUpperCase()
                            .padStart(4, '0'), 680.0 + x * 20, 382.0 + y * 10
                    )
                }
            }
            drawer.fontMap = fontTimeCode
            drawer.text(program.frameCount.toString().padStart(7, '0'), 879.0, 384.0)
            drawer.text(formatDuration(seconds.toInt()), 879.0, 384.0 + 12.0)
            drawer.text(
                (floor(bars) + 1).toInt().toString()
                    .padStart(3, '0')
                    .padStart(5, ' ') + "." + ((floor(bars * 4.0) % 4).toInt() + 1).toString(),
                879.0,
                384.0 + 24.0
            )
            val time = bars * 0.125
            val value = (Hud.getTimeMapper(time.toInt()))(time)
            drawer.drawStyle.clip = Rectangle(758.0, 376.0, 56.0, 128.0)
            drawer.image(txt, 758.0, 376.0 - 384.0 * (value - floor(value)), 56.0, 512.0)
            drawer.drawStyle.clip = null
            drawer.drawStyle.colorMatrix = heroColorMatrix
            drawer.image(
                hero,
                Rectangle(32.0 * (floor(bars * 4.0 * 7.0 + 1.0) % 7), 0.0, 32.0, 32.0),
                Rectangle(886.0 - 16.0, 468.0 - 16.0, 32.0, 32.0)
            )
            drawer.image(rt3D.colorBuffer(0), 40.0, 376.0, 128.0, 128.0)
        }
        bloom.apply(rt.colorBuffer(0), blurred)
        val timedInterval = max(ceil(1.0 - mod(bars, 8.0)), 0.0)
        chromaticAberration.aberrationFactor =
            0.5 + cos(bars * PI * 0.5).pow(256.0) * 4.0 * timedInterval
        chromaticAberration.apply(blurred, blurred)
        drawer.stroke = null
        drawer.fill = ColorRGBa(0.0, 0.0, 0.0, backgroundAlpha)
        drawer.rectangle(24.0, 360.0, 912.0, 160.0)
        drawer.image(blurred)
        drawer.fill = rgBa.opacify(0.4)
        drawer.stroke = null
        val htL = normDb(transform.peakDb(0)) * 125.0
        val htR = normDb(transform.peakDb(1)) * 125.0
        drawer.rectangle(578.0, 503.0 - htL, 5.0, htL)
        drawer.rectangle(594.0, 503.0 - htR, 5.0, htR)
        val fadeInTime = 10.0
        val fadeOutTime = 10.0
        val alphaInv = clamp(
            (seconds - (duration - fadeOutTime)) / fadeOutTime,
            0.0,
            1.0
        ) + clamp(
            1.0 - seconds / fadeInTime,
            0.0,
            1.0
        )
        if (0.0 < alphaInv) {
            drawer.fill = ColorRGBa(0.0, 0.0, 0.0, alphaInv)
            drawer.rectangle(Rectangle(0.0, 0.0, width.toDouble(), height.toDouble()))
        }
    }

    private fun createWaveform() = Waveform(128.0, 40.0)

    private fun createSpectrum() = Spectrum(26, 21, 4, 2, 1)
}