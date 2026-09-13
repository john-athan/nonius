package dev.kaleve.nonius.core

import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.tan

/**
 * The two surface angles a spirit level shows, in degrees.
 *
 * Signs follow the physical device rather than a convention. Face up on a table
 * the gravity sensor reads (0, 0, +9.81); raising the right edge drives x
 * positive and raising the top edge drives y positive. So a positive [roll]
 * means the right edge is high and a positive [pitch] means the top edge is
 * high, which is the side a real bubble floats to.
 */
data class Tilt(val pitch: Float, val roll: Float) {

    val magnitude: Float get() = hypot(pitch, roll)

    operator fun minus(other: Tilt) = Tilt(pitch - other.pitch, roll - other.roll)

    companion object {
        val Level = Tilt(0f, 0f)

        fun fromGravity(x: Float, y: Float, z: Float): Tilt {
            val pitch = Math.toDegrees(atan2(y.toDouble(), hypot(x.toDouble(), z.toDouble())))
            val roll = Math.toDegrees(atan2(x.toDouble(), hypot(y.toDouble(), z.toDouble())))
            return Tilt(pitch.toFloat(), roll.toFloat())
        }
    }
}

/** How close to level counts as level. Half of what the sensor can resolve. */
const val LEVEL_TOLERANCE_DEGREES = 0.15f

fun Tilt.isLevel() = magnitude < LEVEL_TOLERANCE_DEGREES

/**
 * The three scales printed on real levels. Degrees for angles, percent for
 * roads and ramps, millimetres per metre for anyone laying a floor or a drain.
 */
enum class Slope(val suffix: String, val label: String) {
    Degrees("°", "deg"),
    Percent("%", "%"),
    MmPerMetre("mm/m", "mm/m");

    fun of(degrees: Float): Float = when (this) {
        Degrees -> degrees
        Percent -> (tan(Math.toRadians(degrees.toDouble())) * 100.0).toFloat()
        MmPerMetre -> (tan(Math.toRadians(degrees.toDouble())) * 1000.0).toFloat()
    }

    fun format(degrees: Float): String {
        val value = of(degrees)
        val decimals = if (this == MmPerMetre) 0 else 1
        // A leading minus that appears and disappears makes the number jump, so
        // the sign is always there and a rounded zero is written without one.
        val rounded = String.format(Locale.US, "%.${decimals}f", abs(value))
        val sign = if (value < 0 && rounded.any { it in '1'..'9' }) "-" else ""
        return sign + rounded
    }

    fun next(): Slope = entries[(ordinal + 1) % entries.size]
}
