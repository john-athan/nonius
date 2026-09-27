package dev.kaleve.nonius.core

import kotlin.math.sqrt

/** What a G-force meter shows one g as: the accelerometer includes gravity,
 *  so a phone lying still already reads this, not zero. */
const val STANDARD_GRAVITY = 9.80665f

/** Magnitude of the three accelerometer axes, gravity included, as a ratio to it. */
fun gForce(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY
