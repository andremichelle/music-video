package audio

import kotlin.math.ln

fun secondsToNumFrames(seconds: Double, samplingRate: Int): Double {
    return seconds * samplingRate
}

private val DB_LV = ln(10.0) / 20.0

fun gainToDb(gain: Float): Double {
    return ln(gain) / DB_LV
}