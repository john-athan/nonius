package dev.kaleve.nonius.tools

import android.hardware.Sensor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.kaleve.nonius.core.Slope
import dev.kaleve.nonius.core.Tilt
import dev.kaleve.nonius.core.isLevel
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.sensor.rememberReading
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.hair
import dev.kaleve.nonius.ui.line
import dev.kaleve.nonius.ui.palette
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Full deflection of the round vial, in degrees. A joiner works inside two. */
private const val BULLSEYE_RANGE = 8f

/** The tube is the fine scale: a fifth of the range of the round one. */
private const val VIAL_RANGE = 1.6f

@Composable
fun LevelScreen(onBack: () -> Unit) {
    val reading = rememberReading(Sensor.TYPE_GRAVITY)
    var unit by rememberSaveable { mutableStateOf(Slope.Degrees) }
    var zeroPitch by rememberSetting("level.zero.pitch", 0f)
    var zeroRoll by rememberSetting("level.zero.roll", 0f)
    var held by remember { mutableStateOf<Tilt?>(null) }

    val raw = reading?.let { Tilt.fromGravity(it[0], it[1], it[2]) }
    val live = (raw ?: Tilt.Level) - Tilt(zeroPitch, zeroRoll)
    val tilt = held ?: live

    // The bubble has mass. Critically damped, so it settles without wobbling
    // around the mark like a cheap animation.
    val shownPitch by animateFloatAsState(
        tilt.pitch, spring(1f, Spring.StiffnessMedium), label = "pitch",
    )
    val shownRoll by animateFloatAsState(
        tilt.roll, spring(1f, Spring.StiffnessMedium), label = "roll",
    )

    val level = tilt.isLevel()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(level) {
        // A level lying under a shelf is read by feel, not by eye.
        if (level) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Instrument(
        title = "Level",
        onBack = onBack,
        trailing = unit.label,
        onTrailingClick = { unit = unit.next() },
        footnote = when {
            raw == null -> "waiting for the motion sensor"
            held != null -> "holding the last reading"
            else -> null
        },
        actions = {
            Action("Zero") { raw?.let { zeroPitch = it.pitch; zeroRoll = it.roll } }
            Action("Hold", latched = held != null) { held = if (held == null) live else null }
        },
    ) {
        Bullseye(
            shownPitch, shownRoll, level,
            Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 8.dp),
        )
        Vial(shownRoll, level, Modifier.fillMaxWidth().height(46.dp).padding(top = 8.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(unit.format(tilt.pitch), unit.suffix, "pitch", emphasis = level)
            Reading(unit.format(tilt.roll), unit.suffix, "roll", emphasis = level)
        }
    }
}

/**
 * The round vial. Ticks every 7.5 degrees so the rim reads as a scale rather
 * than decoration, and the bubble is held inside the glass the way a real one is.
 */
@Composable
private fun Bullseye(pitch: Float, roll: Float, level: Boolean, modifier: Modifier) {
    val rule = palette.rule
    val rule2 = palette.rule2
    val ink2 = palette.ink2
    val ink3 = palette.ink3
    val glass = palette.glass
    val paper = palette.paper
    val found = palette.level

    Canvas(modifier) {
        val centre = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - line(2f)
        drawCircle(rule, radius, centre, style = hair())
        drawCircle(rule, radius * 0.74f, centre, style = hair())
        drawCircle(rule, radius * 0.48f, centre, style = hair())

        var angle = 0f
        while (angle < 360f) {
            val radians = Math.toRadians(angle.toDouble())
            val inner = if (angle % 45f == 0f) radius * 0.86f else radius * 0.93f
            val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
            drawLine(ink3, centre + direction * inner, centre + direction * radius, line(0.7f))
            angle += 7.5f
        }
        val cross = radius * 0.08f
        drawLine(ink3, centre + Offset(-radius, 0f), centre + Offset(-radius + cross, 0f), line())
        drawLine(ink3, centre + Offset(radius, 0f), centre + Offset(radius - cross, 0f), line())
        drawLine(ink3, centre + Offset(0f, -radius), centre + Offset(0f, -radius + cross), line())
        drawLine(ink3, centre + Offset(0f, radius), centre + Offset(0f, radius - cross), line())

        val bubble = radius * 0.22f
        val travel = radius * 0.62f
        var x = (roll / BULLSEYE_RANGE).coerceIn(-1f, 1f) * travel
        var y = -(pitch / BULLSEYE_RANGE).coerceIn(-1f, 1f) * travel
        val reach = hypot(x, y)
        if (reach > travel) {
            x = x / reach * travel
            y = y / reach * travel
        }
        val seat = centre + Offset(x, y)

        // The eye: two rings at the centre that the bubble has to sit between.
        drawCircle(if (level) found else rule2, bubble * 1.22f, centre, style = hair(1.3f))
        drawCircle(glass.copy(alpha = 0.55f), bubble, seat)
        drawCircle(if (level) found else ink2, bubble, seat, style = hair(1f))
        drawCircle(paper.copy(alpha = 0.5f), bubble * 0.3f, seat + Offset(-bubble * 0.34f, -bubble * 0.4f))
    }
}

/** The tube, for the last tenth of a degree once the round vial is centred. */
@Composable
private fun Vial(roll: Float, level: Boolean, modifier: Modifier) {
    val rule2 = palette.rule2
    val ink2 = palette.ink2
    val ink3 = palette.ink3
    val glass = palette.glass
    val found = palette.level

    Canvas(modifier) {
        val height = size.height
        val radius = height / 2
        drawRoundRect(
            rule2, Offset.Zero, Size(size.width, height), CornerRadius(radius), style = hair(),
        )
        val bubble = radius * 0.72f
        val travel = size.width / 2 - radius - bubble * 0.4f
        val centre = Offset(
            size.width / 2 + (roll / VIAL_RANGE).coerceIn(-1f, 1f) * travel,
            radius,
        )
        var tick = -10
        while (tick <= 10) {
            val x = size.width / 2 + tick / 10f * travel
            val long = tick % 5 == 0
            drawLine(
                ink3,
                Offset(x, radius - (if (long) radius * 0.55f else radius * 0.3f)),
                Offset(x, radius + (if (long) radius * 0.55f else radius * 0.3f)),
                line(0.6f),
            )
            tick++
        }
        val gate = bubble * 1.35f
        drawLine(ink2, Offset(size.width / 2 - gate, 2f), Offset(size.width / 2 - gate, height - 2f), line())
        drawLine(ink2, Offset(size.width / 2 + gate, 2f), Offset(size.width / 2 + gate, height - 2f), line())
        drawCircle(glass.copy(alpha = 0.55f), bubble, centre)
        drawCircle(if (level) found else ink2, bubble, centre, style = hair(1f))
    }
}
