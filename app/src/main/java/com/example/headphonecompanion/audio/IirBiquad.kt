package com.example.headphonecompanion.audio

/**
 * Simple Biquad IIR implementation for a single second-order section.
 * Coefficients follow the Direct Form I (DF1) difference equation:
 * y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
 *
 * This implementation is intended for educational/POC purposes. For production use
 * consider using a highly-optimized native implementation (NDK) and take care of
 * numerical stability and performance.
 */
class Biquad(
    var b0: Float = 1f,
    var b1: Float = 0f,
    var b2: Float = 0f,
    var a1: Float = 0f,
    var a2: Float = 0f
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    fun process(sample: Float): Float {
        val y = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = sample
        y2 = y1
        y1 = y
        return y
    }

    companion object {
        /**
         * Creates a peaking EQ biquad section (peak/notch) using center frequency, Q and gain (dB).
         * Reference: RBJ cookbook formula.
         */
        fun peaking(fs: Float, f0: Float, q: Float, gainDb: Float): Biquad {
            val A = Math.pow(10.0, (gainDb / 40.0)).toFloat()
            val omega = (2.0 * Math.PI * f0 / fs).toFloat()
            val alpha = (Math.sin(omega) / (2.0 * q)).toFloat()
            val cosw = Math.cos(omega.toDouble()).toFloat()

            val b0 = 1f + alpha * A
            val b1 = -2f * cosw
            val b2 = 1f - alpha * A
            val a0 = 1f + alpha / A
            val a1 = -2f * cosw
            val a2 = 1f - alpha / A

            return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }

        /**
         * Creates a low/high shelving biquad (not used by default here) — kept for future.
         */
    }
}
