package dev.kaleve.nonius.core

import kotlin.math.log10
import kotlin.math.max
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
 * a meter they trust. Unweighted, so it is dB(Z) and never claims to be dB(A).
 */
fun soundPressureLevel(rms: Float, offsetDb: Float): Float = dbFullScale(rms) + offsetDb

// ponytail: unweighted only. A-weighting is a biquad cascade, some thirty lines
// and a test against the IEC 61672 curve. Until that exists the reading says
// dB(Z) on screen rather than claiming a weighting it does not apply.
const val DEFAULT_SPL_OFFSET_DB = 100f
