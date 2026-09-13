package dev.kaleve.nonius.core

import kotlin.math.pow

/** Standard atmosphere at sea level. */
const val STANDARD_PRESSURE_HPA = 1013.25f

/**
 * The international barometric formula, the one cast into aircraft altimeters.
 *
 * With [referenceHpa] left at standard it gives height above sea level, which is
 * only as right as the weather. Set the reference to the pressure where you are
 * standing and it gives the height above that point, which is exact enough to
 * measure a staircase.
 */
fun altitudeMetres(pressureHpa: Float, referenceHpa: Float = STANDARD_PRESSURE_HPA): Float =
    (44330.0 * (1.0 - (pressureHpa.toDouble() / referenceHpa).pow(1.0 / 5.255))).toFloat()

/** Falling pressure means weather coming in. Under a hectopascal an hour is noise. */
fun pressureTrend(hpaPerHour: Float): String = when {
    hpaPerHour > 1f -> "rising"
    hpaPerHour < -1f -> "falling"
    else -> "steady"
}
