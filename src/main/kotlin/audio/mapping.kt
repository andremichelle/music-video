package audio

import org.openrndr.math.clamp
import kotlin.math.ln

fun secondsToNumFrames(seconds: Double, samplingRate: Int): Double {
    return seconds * samplingRate
}

fun secondsToBars(seconds: Double, bpm: Double): Double {
    return bpm * seconds / 240.0
}

private val DB_LV = ln(10.0) / 20.0

fun gainToDb(gain: Float): Double {
    return ln(gain) / DB_LV
}

fun normDb(minDb: Double = -72.0, maxDb: Double = 0.0): (Double) -> Double {
    return { db -> clamp((db - minDb) / (maxDb - minDb), 0.0, 1.0) }
}