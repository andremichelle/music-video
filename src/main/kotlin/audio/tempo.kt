package audio

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
class TempoEvent(
    private val position: Int,
    private val location: Int,
    val interpolation: Int,
    val value: Double,
    val slope: Double
) {
    @OptIn(UnstableDefault::class)
    companion object {
        fun fetch(key: String): List<TempoEvent> {
            val path = "/Users/andre.michelle/Documents/Audiotool/Mixes/cache/tempo/${key}.json"
            return try {
                Json.parse(serializer().list, File(path).readText())
            } catch (throwable: Throwable) {
                println("Could not find additional tempo-automation. Using default bpm.")
                emptyList()
            }
        }
    }

    fun bars(): Double {
        return position / 15360.0
    }

    override fun toString(): String {
        return "TempoEvent(bars=${bars()}, location=$location, interpolation=$interpolation, value=$value, slope=$slope)"
    }
}

class TempoEvaluator(private val events: List<TempoEvent>, defaultBpm: Double) {
    private val expStepper = ExpStepper()
    private val resolution = 1.0 / 16.0
    private val numSubSteps = 16
    private val subResolution = resolution / numSubSteps

    private val iterator: Iterator<TempoEvent> = events.iterator()
    private var curr: TempoEvent? = if (iterator.hasNext()) iterator.next() else null
    private var next: TempoEvent? = if (iterator.hasNext()) iterator.next() else null
    private var bars = 0.0
    private var time = 0.0
    private var bpm = curr?.value ?: defaultBpm
    private var subIndex = 0

    fun advance(target: Double) {
        if (events.isEmpty() || null == curr) {
            bars = secondsToBars(target, bpm)
            time = target
        } else {
            while (time < target) {
                bars = ++subIndex * subResolution
                time += barsToSeconds(subResolution, bpm)
                if ((subIndex % numSubSteps) == 0) { // next resolution step
                    bpm = evaluate(bars)
                }
                if (null != next) {
                    while (bars >= next!!.bars()) {
                        curr = next
                        if (iterator.hasNext()) {
                            next = iterator.next()
                        } else {
                            next = null
                            break
                        }
                    }
                }
            }
        }
    }

    private fun evaluate(bars: Double): Double {
        return if (curr!!.interpolation == 0 || next == null) {
            curr!!.value
        } else {
            val ay: Double = curr!!.value
            val by: Double = next!!.value
            val dy = by - ay
            if (0.0 == dy) {
                curr!!.value
            } else {
                val ax: Double = curr!!.bars()
                val bx: Double = next!!.bars()
                val dx = bx - ax
                val slope = curr!!.slope
                if (0.5 == slope) {
                    ay + dy / dx * (bars - ax)
                } else {
                    expStepper.bySlope(bx - ax, ay, slope, by)
                    expStepper.y(bars - ax)
                }
            }
        }
    }

    fun bars(): Double {
        return bars
    }

    fun bpm(): Double {
        return bpm
    }
}