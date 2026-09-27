package dev.kaleve.nonius.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Root mean square of one frame of samples, each in -1.0 to 1.0. */
fun rms(samples: FloatArray): Float {
    if (samples.isEmpty()) return 0f
    var sum = 0.0
    for (v in samples) sum += v.toDouble() * v
    return sqrt(sum / samples.size).toFloat()
}

/** Level below digital full scale. Silence is floored rather than infinite. */
fun dbFullScale(rms: Float): Float = 20f * log10(max(rms, 1e-7f))

/**
 * No phone microphone is calibrated and none is flat, so this is a sum and not
 * a measurement: the level below full scale plus an offset the user sets against
 * a meter they trust. The offset corrects the microphone's gain alone, which a
 * weighting curve does not touch, so the same offset serves dB(A) and dB(Z).
 */
fun soundPressureLevel(rms: Float, offsetDb: Float): Float = dbFullScale(rms) + offsetDb

const val DEFAULT_SPL_OFFSET_DB = 100f

/**
 * One IIR stage, direct form I, up to two real poles and two real zeros. Every
 * pole an A-weighting filter needs is real, so this is the only shape a stage
 * has to take, no complex arithmetic anywhere.
 */
private class Biquad(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    /** Magnitude at one angular frequency, used once to find the 1 kHz gain. */
    fun gainAt(w: Double): Double {
        val zr = cos(w); val zi = -sin(w)  // z^-1 = e^(-jw)
        val z2r = zr * zr - zi * zi
        val z2i = 2.0 * zr * zi
        val numR = b0 + b1 * zr + b2 * z2r
        val numI = b1 * zi + b2 * z2i
        val denR = 1.0 + a1 * zr + a2 * z2r
        val denI = a1 * zi + a2 * z2i
        return sqrt(numR * numR + numI * numI) / sqrt(denR * denR + denI * denI)
    }
}

/**
 * IEC 61672-1 A-weighting, three biquads from the bilinear transform of the
 * standard analog poles (20.6 Hz and 12194 Hz doubled, 107.7 Hz and 737.9 Hz
 * single). A real analog pole at -p maps to a real digital pole at
 * (2*fs-p)/(2*fs+p), so every pole here lands on the real axis and no stage
 * needs a complex conjugate pair.
 *
 * The four zeros the analog filter has at the origin, and the two more that
 * bilinear transform puts at Nyquist for the two degrees by which the
 * denominator outgrows the numerator, are split two ways: the double poles at
 * 20.6 Hz and 12194 Hz each take two of the origin zeros, and the single poles
 * at 107.7 Hz and 737.9 Hz share the Nyquist pair. Cascading in any order gives
 * the same filter; only the gain is set separately, at the end, so 1 kHz reads
 * 0 dB on this exact instance rather than on the ideal analog prototype.
 */
class AWeighting(sampleRate: Int) {
    private val low: Biquad
    private val high: Biquad
    private val mid: Biquad
    private val gain: Double

    init {
        val k = 2.0 * sampleRate
        fun digitalPole(analogHz: Double): Double {
            val w = 2.0 * PI * analogHz
            return (k - w) / (k + w)
        }
        val p1 = digitalPole(20.598997)
        val p4 = digitalPole(12194.217)
        val p2 = digitalPole(107.65265)
        val p3 = digitalPole(737.86223)

        low = Biquad(1.0, -2.0, 1.0, -2.0 * p1, p1 * p1)
        high = Biquad(1.0, -2.0, 1.0, -2.0 * p4, p4 * p4)
        mid = Biquad(1.0, 2.0, 1.0, -(p2 + p3), p2 * p3)

        val w1000 = 2.0 * PI * 1000.0 / sampleRate
        val unnormalised = low.gainAt(w1000) * high.gainAt(w1000) * mid.gainAt(w1000)
        gain = 1.0 / unnormalised
    }

    fun process(sample: Float): Float =
        (gain * mid.process(high.process(low.process(sample.toDouble())))).toFloat()
}

/**
 * Peak and minimum across a run of readings, reset together. The minimum
 * ignores the first half second after a reset: a filter that has just started
 * or just been reset has not settled, and would otherwise pin the minimum at
 * whatever low, meaningless value comes out before it does.
 */
class HoldTracker {
    private var since = 0L
    var peak: Float = 0f
        private set
    var minimum: Float = Float.NaN
        private set

    fun reset(atMillis: Long) {
        since = atMillis
        peak = 0f
        minimum = Float.NaN
    }

    fun update(value: Float, atMillis: Long) {
        peak = max(peak, value)
        if (atMillis - since >= SETTLE_MS) {
            minimum = if (minimum.isNaN()) value else min(minimum, value)
        }
    }

    private companion object {
        const val SETTLE_MS = 500L
    }
}
