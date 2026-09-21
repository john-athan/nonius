package dev.kaleve.nonius.core

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Which of the four cardinal ways a device is being held up, named by the edge
 * that points against gravity. A level tube reads the axis that runs across
 * the screen, and that axis is a different device axis in each: see [Tilt]'s
 * gravity convention for the sign of pitch and roll this maps onto.
 */
enum class Posture { PortraitUp, LandscapeRight, PortraitDown, LandscapeLeft }

/**
 * Below this fraction of gravity lying in the screen's plane there is no
 * posture to read: the device is closer to flat on a table than to held up.
 * 0.6 is roughly 37 degrees off flat.
 */
private const val IN_PLANE_FRACTION = 0.6f

/**
 * How far past the neutral 45 degree boundary a posture must swing before it
 * gives way to a neighbour. Without this a device parked near a boundary would
 * flip on sensor noise alone; the current posture gets a wider berth than the
 * others so leaving it takes a clearly larger angle than staying does.
 */
private const val HYSTERESIS_DEGREES = 20f

private val Posture.centreDegrees: Float
    get() = when (this) {
        Posture.LandscapeRight -> 0f
        Posture.PortraitUp -> 90f
        Posture.LandscapeLeft -> 180f
        Posture.PortraitDown -> 270f
    }

private fun angularDistance(a: Float, b: Float): Float {
    val d = Math.abs(a - b) % 360f
    return if (d > 180f) 360f - d else d
}

/**
 * The posture a gravity reading names, or null when the reading says nothing
 * because the device is too close to flat. [current] is the posture already in
 * force, if any, and is favoured by [HYSTERESIS_DEGREES] so a steady hold near
 * a boundary settles instead of oscillating.
 */
fun postureFromGravity(x: Float, y: Float, z: Float, current: Posture? = null): Posture? {
    val inPlane = hypot(x, y)
    val magnitude = hypot(inPlane, z)
    if (magnitude <= 0f || inPlane / magnitude < IN_PLANE_FRACTION) return null

    val angle = (Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat() + 360f) % 360f
    return Posture.entries.minBy { posture ->
        val distance = angularDistance(angle, posture.centreDegrees)
        if (posture == current) distance - HYSTERESIS_DEGREES else distance
    }
}

/** How long a candidate has to hold before it replaces the posture in force. */
private const val DWELL_MS = 1200L

/**
 * The posture actually shown. A candidate only takes over once it has held
 * continuously for [DWELL_MS]; a flick that reverses before then never counts
 * as having held at all, so the latch does not need to remember it happened.
 */
class PostureLatch(initial: Posture = Posture.PortraitUp) {
    var posture: Posture = initial
        private set
    private var pending: Posture? = null
    private var pendingSinceMs: Long = 0L

    fun update(candidate: Posture?, atMs: Long): Posture {
        if (candidate == null || candidate == posture) {
            pending = null
        } else if (candidate != pending) {
            pending = candidate
            pendingSinceMs = atMs
        } else if (atMs - pendingSinceMs >= DWELL_MS) {
            posture = candidate
            pending = null
        }
        return posture
    }
}
