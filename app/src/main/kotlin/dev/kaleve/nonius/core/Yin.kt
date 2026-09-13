package dev.kaleve.nonius.core

import kotlin.math.max
import kotlin.math.min

/**
 * Pitch by the YIN difference function.
 *
 * A guitar string is not a sine, and the loudest thing in its spectrum is often
 * not its fundamental, which is why a peak picker hears the wrong octave. YIN
 * asks a different question, how far the signal is from a copy of itself
 * shifted by tau, and takes the first shift that nearly cancels. That first dip
 * is the fundamental even when it is quieter than its overtones.
 *
 * Returns null when nothing periodic is there, which is what silence sounds like.
 */
fun detectPitchHz(
    samples: FloatArray,
    sampleRate: Int,
    threshold: Float = 0.12f,
    minHz: Float = 55f,
    maxHz: Float = 1600f,
): Float? {
    val window = samples.size / 2
    val tauMin = max(2, (sampleRate / maxHz).toInt())
    val tauMax = min(window, (sampleRate / minHz).toInt())
    if (window < 2 || tauMax <= tauMin + 1) return null

    // ponytail: O(window * tau) per frame, about a million multiplies at 2048
    // samples. Measured well under a frame period on a current phone; the FFT
    // route only pays off if this ever runs on more than one frame at a time.
    val difference = FloatArray(tauMax + 1)
    for (tau in tauMin..tauMax) {
        var sum = 0f
        for (j in 0 until window) {
            val d = samples[j] - samples[j + tau]
            sum += d * d
        }
        difference[tau] = sum
    }

    // The cumulative mean normalisation is what removes the trivial dip at tau 0
    // and puts every candidate on the same scale.
    val normalised = FloatArray(tauMax + 1)
    var running = 0f
    for (tau in tauMin..tauMax) {
        running += difference[tau]
        normalised[tau] =
            if (running <= 0f) 1f else difference[tau] * (tau - tauMin + 1) / running
    }

    var tau = tauMin
    while (tau <= tauMax) {
        if (normalised[tau] < threshold) {
            while (tau + 1 <= tauMax && normalised[tau + 1] < normalised[tau]) tau++
            val period = refine(normalised, tau, tauMin, tauMax)
            return if (period > 0f) sampleRate / period else null
        }
        tau++
    }
    return null
}

/**
 * Parabola through the dip and its two neighbours. Without this the tuner can
 * only land on whole samples, which near the top string is a wrong reading of
 * several cents.
 */
private fun refine(curve: FloatArray, tau: Int, lo: Int, hi: Int): Float {
    if (tau <= lo || tau >= hi) return tau.toFloat()
    val a = curve[tau - 1]
    val b = curve[tau]
    val c = curve[tau + 1]
    val denominator = 2f * (a - 2f * b + c)
    if (denominator == 0f) return tau.toFloat()
    return tau + (a - c) / denominator
}
