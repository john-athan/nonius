package dev.kaleve.nonius.core

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

/** A note name with how far the heard tone sits from it, in cents. */
data class Note(val name: String, val octave: Int, val cents: Float, val hertz: Float) {
    val label: String get() = "$name$octave"
    val inTune: Boolean get() = kotlin.math.abs(cents) < 3f
}

private val NAMES =
    listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

/** Concert pitch is a setting, not a constant: baroque ensembles tune to 415. */
const val DEFAULT_A4_HZ = 440f
val A4_RANGE = 415f..466f

/**
 * The nearest note to a frequency, and the distance to it.
 *
 * Twelve tone equal temperament, so this is one logarithm: semitones from A4,
 * rounded to the nearest note, and the rounding error read back as cents.
 */
fun noteFor(hertz: Float, a4: Float = DEFAULT_A4_HZ): Note? {
    if (hertz <= 0f || a4 <= 0f) return null
    val semitones = 12.0 * ln(hertz / a4.toDouble()) / ln(2.0)
    val nearest = semitones.roundToInt()
    val cents = ((semitones - nearest) * 100.0).toFloat()
    val fromC = nearest + 9                      // A is the tenth name in the octave
    val index = ((fromC % 12) + 12) % 12
    val octave = 4 + floor(fromC / 12.0).toInt()
    return Note(NAMES[index], octave, cents, hertz)
}
