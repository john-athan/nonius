package dev.kaleve.nonius.tools

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.core.A4_RANGE
import dev.kaleve.nonius.core.DEFAULT_A4_HZ
import dev.kaleve.nonius.core.Note
import dev.kaleve.nonius.core.detectPitchHz
import dev.kaleve.nonius.core.noteFor
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.sensor.SAMPLE_RATE
import dev.kaleve.nonius.sensor.rememberMicrophone
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Adjuster
import dev.kaleve.nonius.ui.Body
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.hair
import dev.kaleve.nonius.ui.line
import dev.kaleve.nonius.ui.palette
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** The needle covers a quarter tone either side, which is where tuning happens. */
private const val CENTS_RANGE = 50f

@Composable
fun TunerScreen(onBack: () -> Unit) {
    val microphone = rememberMicrophone()
    val frame = rememberAudioFrame(microphone.granted)
    var concert by rememberSetting("tuner.a4", DEFAULT_A4_HZ)
    var setting by rememberSaveable { mutableStateOf(false) }
    var heard by remember { mutableStateOf<Note?>(null) }

    val note = frame?.let { audio ->
        detectPitchHz(audio.samples, SAMPLE_RATE)?.let { noteFor(it, concert) }
    }
    LaunchedEffect(frame) { if (note != null) heard = note }
    // A plucked string dies away; the reading stays put for a moment instead of
    // blinking out between plucks.
    LaunchedEffect(heard) {
        delay(1600)
        heard = null
    }

    val cents = heard?.cents ?: 0f
    val needle by animateFloatAsState(
        cents.coerceIn(-CENTS_RANGE, CENTS_RANGE),
        spring(0.75f, Spring.StiffnessLow),
        label = "needle",
    )
    val inTune = heard?.inTune == true
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(inTune) {
        if (inTune) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Instrument(
        title = "Tuner",
        onBack = onBack,
        trailing = String.format(Locale.US, "A %.0f", concert),
        onTrailingClick = { setting = !setting },
        footnote = when {
            !microphone.granted -> null
            heard == null -> "play a note"
            else -> null
        },
        actions = {
            if (microphone.granted) {
                Action("Pitch", latched = setting) { setting = !setting }
            } else {
                Action("Allow microphone") { microphone.ask() }
            }
        },
    ) {
        if (!microphone.granted) {
            Body(
                "The tuner listens for one note at a time and reports how far it sits " +
                    "from the nearest one. Nothing is recorded or sent."
            )
            return@Instrument
        }
        Dial(needle, inTune, heard != null, Modifier.fillMaxWidth().aspectRatio(1.55f))
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText(
                heard?.label ?: "--",
                style = Type.reading.copy(
                    color = if (inTune) palette.level else palette.ink,
                    fontSize = 64.sp,
                ),
            )
            Reading(
                heard?.let { String.format(Locale.US, "%+.0f", it.cents) } ?: "--",
                "cents", "deviation", emphasis = inTune, style = Type.readingSmall,
            )
        }
        if (setting) {
            Adjuster(
                value = concert,
                range = A4_RANGE,
                label = "concert pitch",
                display = String.format(Locale.US, "%.1f Hz", concert),
                onChange = { concert = it },
                onReset = { concert = DEFAULT_A4_HZ },
            )
        }
    }
}

@Composable
private fun Dial(cents: Float, inTune: Boolean, listening: Boolean, modifier: Modifier) {
    val rule = palette.rule
    val ink2 = palette.ink2
    val ink3 = palette.ink3
    val signal = palette.signal
    val found = palette.level

    Canvas(modifier) {
        val pivot = Offset(size.width / 2, size.height * 0.94f)
        val radius = size.height * 0.82f
        val sweep = 62f

        drawArc(
            rule,
            startAngle = 180f + (90f - sweep),
            sweepAngle = sweep * 2,
            useCenter = false,
            topLeft = Offset(pivot.x - radius, pivot.y - radius),
            size = Size(radius * 2, radius * 2),
            style = hair(),
        )

        var mark = -CENTS_RANGE
        while (mark <= CENTS_RANGE) {
            val angle = Math.toRadians((mark / CENTS_RANGE * sweep - 90f).toDouble())
            val major = mark.toInt() % 25 == 0
            val outer = radius
            val inner = radius - (if (major) 16f else 9f) * density
            val direction = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
            drawLine(
                if (major) ink2 else ink3,
                pivot + Offset(direction.x * inner, direction.y * inner),
                pivot + Offset(direction.x * outer, direction.y * outer),
                line(if (major) 1f else 0.7f),
            )
            mark += 5f
        }

        // The window at dead centre is the target, not the needle.
        val gate = Math.toRadians((3f / CENTS_RANGE * sweep - 90f).toDouble())
        val gateDirection = Offset(sin(gate).toFloat(), -cos(gate).toFloat())
        listOf(-1f, 1f).forEach { side ->
            drawLine(
                if (inTune) found else rule,
                pivot + Offset(side * gateDirection.x * radius * 0.62f, gateDirection.y * radius * 0.62f),
                pivot + Offset(side * gateDirection.x * radius, gateDirection.y * radius),
                line(1f),
            )
        }

        if (listening) {
            val angle = Math.toRadians((cents / CENTS_RANGE * sweep - 90f).toDouble())
            val tip = pivot + Offset(
                sin(angle).toFloat() * radius * 0.95f,
                -cos(angle).toFloat() * radius * 0.95f,
            )
            drawLine(if (inTune) found else signal, pivot, tip, line(1.6f))
            drawCircle(if (inTune) found else signal, line(3.5f), pivot)
        }
    }
}
