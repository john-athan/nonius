package dev.kaleve.nonius.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kaleve.nonius.core.AWeighting
import dev.kaleve.nonius.core.DEFAULT_SPL_OFFSET_DB
import dev.kaleve.nonius.core.rms
import dev.kaleve.nonius.core.soundPressureLevel
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.sensor.SAMPLE_RATE
import dev.kaleve.nonius.sensor.rememberMicrophone
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Adjuster
import dev.kaleve.nonius.ui.Body
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.Trace
import dev.kaleve.nonius.ui.Type
import java.util.Locale
import kotlin.math.max

private const val TRACE_LENGTH = 160

@Composable
fun SoundScreen(onBack: () -> Unit) {
    val microphone = rememberMicrophone()
    val frame = rememberAudioFrame(microphone.granted)
    var offset by rememberSetting("sound.offset", DEFAULT_SPL_OFFSET_DB)
    var weighted by rememberSetting("sound.weighted", true)
    var calibrating by rememberSaveable { mutableStateOf(false) }
    // Kept running regardless of the toggle, so switching from Z to A never
    // shows the settling transient of a filter that has been sitting idle.
    val filter = remember { AWeighting(SAMPLE_RATE) }
    var peak by remember { mutableFloatStateOf(0f) }
    var trace by remember { mutableStateOf(FloatArray(0)) }

    val level = frame?.let {
        val aWeighted = rms(FloatArray(it.samples.size) { i -> filter.process(it.samples[i]) })
        soundPressureLevel(if (weighted) aWeighted else rms(it.samples), offset)
    }
    LaunchedEffect(frame) {
        val reading = level ?: return@LaunchedEffect
        peak = max(peak, reading)
        val kept = if (trace.size >= TRACE_LENGTH) trace.copyOfRange(1, trace.size) else trace
        trace = kept + reading
    }

    val unit = if (weighted) "dB(A)" else "dB(Z)"
    Instrument(
        title = "Sound level",
        onBack = onBack,
        trailing = frame?.let { if (it.unprocessed) "raw mic" else "processed mic" },
        footnote = when {
            !microphone.granted -> null
            frame == null -> "opening the microphone"
            frame.unprocessed -> "Set the offset against a meter you trust, at 1 kHz or with broadband noise."
            else -> "This device applies gain control, so the reading drifts under it."
        },
        actions = {
            if (microphone.granted) {
                Action(unit, latched = weighted) { weighted = !weighted }
                Action("Calibrate", latched = calibrating) { calibrating = !calibrating }
                Action("Reset peak") { peak = 0f; trace = FloatArray(0) }
            } else {
                Action("Allow microphone") { microphone.ask() }
            }
        },
    ) {
        if (!microphone.granted) {
            Body(
                "A sound level meter has to listen. Nothing is recorded, kept or sent: " +
                    "each frame is turned into one number and thrown away."
            )
            return@Instrument
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(
                level?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                unit, "now",
            )
            Reading(
                if (peak > 0f) String.format(Locale.US, "%.1f", peak) else "--",
                unit, "peak", style = Type.readingSmall,
            )
        }
        Trace(
            trace, 30f..110f,
            Modifier.fillMaxWidth().height(110.dp).padding(top = 18.dp),
        )
        if (calibrating) {
            Adjuster(
                value = offset,
                range = 70f..130f,
                label = "microphone offset",
                display = String.format(Locale.US, "%+.1f dB", offset),
                onChange = { offset = it },
                onReset = { offset = DEFAULT_SPL_OFFSET_DB },
            )
        }
    }
}
