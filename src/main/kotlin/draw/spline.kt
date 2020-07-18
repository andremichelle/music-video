package draw

import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

class BezierSpline(knots: Int) {
    private val mKnots: Int

    private val mX: FloatArray
    private val mY: FloatArray

    private val mPX1: FloatArray
    private val mPY1: FloatArray
    private val mPX2: FloatArray
    private val mPY2: FloatArray

    private var mResolved: Boolean = false
    private var mResolver: ControlPointsResolver = ControlPointsResolver(knots - 1)

    init {
        if (knots <= 1) {
            throw IllegalArgumentException("At least two knot points required")
        }

        mKnots = knots
        mX = FloatArray(knots)
        mY = FloatArray(knots)

        val segments = knots - 1
        mPX1 = FloatArray(segments)
        mPY1 = FloatArray(segments)
        mPX2 = FloatArray(segments)
        mPY2 = FloatArray(segments)
    }

    fun set(knot: Int, x: Float, y: Float) {
        mX[knot] = x
        mY[knot] = y
        mResolved = false
    }

    fun set(knot: Int, x: Double, y: Double) {
        mX[knot] = x.toFloat()
        mY[knot] = y.toFloat()
        mResolved = false
    }

    fun create(): ShapeContour {
        ensureResolved()

        return contour {
            moveTo(mX[0].toDouble(), mY[0].toDouble())
            val segments = mKnots - 1
            if (segments == 1) {
                lineTo(mX[1].toDouble(), mY[1].toDouble())
            } else {
                for (segment in 0.until(segments)) {
                    val knot = segment + 1
                    curveTo(
                        mPX1[segment].toDouble(),
                        mPY1[segment].toDouble(),
                        mPX2[segment].toDouble(),
                        mPY2[segment].toDouble(),
                        mX[knot].toDouble(),
                        mY[knot].toDouble()
                    )
                }
            }
        }
    }

    private fun ensureResolved() {
        if (!mResolved) {
            val segments = mKnots - 1
            if (segments == 1) {
                mPX1[0] = mX[0]
                mPY1[0] = mY[0]
                mPX2[0] = mX[1]
                mPY2[0] = mY[1]
            } else {
                mResolver.resolve(mX, mPX1, mPX2)
                mResolver.resolve(mY, mPY1, mPY2)
            }
            mResolved = true
        }
    }

    private class ControlPointsResolver internal constructor(private val mSegments: Int) {
        private val mA: FloatArray = FloatArray(mSegments)
        private val mB: FloatArray = FloatArray(mSegments)
        private val mC: FloatArray = FloatArray(mSegments)
        private val mR: FloatArray = FloatArray(mSegments)
        fun resolve(K: FloatArray, P1: FloatArray, P2: FloatArray) {
            val segments = mSegments
            val last = segments - 1
            val a = mA
            val b = mB
            val c = mC
            val d = mR

            // prepare left most segment.
            a[0] = 0f
            b[0] = 2f
            c[0] = 1f
            d[0] = K[0] + 2f * K[1]

            // prepare internal segments.
            for (i in 1 until last) {
                a[i] = 1f
                b[i] = 4f
                c[i] = 1f
                d[i] = 4f * K[i] + 2f * K[i + 1]
            }

            // prepare right most segment.
            a[last] = 2f
            b[last] = 7f
            c[last] = 0f
            d[last] = 8f * K[last] + K[segments]

            // solves Ax=b with the Thomas algorithm (from Wikipedia).
            for (i in 1 until segments) {
                val m = a[i] / b[i - 1]
                b[i] = b[i] - m * c[i - 1]
                d[i] = d[i] - m * d[i - 1]
            }
            P1[last] = d[last] / b[last]
            for (i in segments - 2 downTo 0) {
                P1[i] = (d[i] - c[i] * P1[i + 1]) / b[i]
            }

            // we have p1, now compute p2.
            for (i in 0 until last) {
                P2[i] = 2f * K[i + 1] - P1[i + 1]
            }
            P2[last] = (K[segments] + P1[segments - 1]) / 2f
        }
    }
}