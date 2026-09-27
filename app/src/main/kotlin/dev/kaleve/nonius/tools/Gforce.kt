package dev.kaleve.nonius.tools

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kaleve.nonius.core.HoldTracker
import dev.kaleve.nonius.core.STANDARD_GRAVITY
import dev.kaleve.nonius.core.gForce
import dev.kaleve.nonius.sensor.rememberReading
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Micro
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.Trace
import dev.kaleve.nonius.ui.Type
import java.util.Locale

private const val TRACE_LENGTH = 160

@Composable
fun GforceScreen(onBack: () -> Unit) {
    val reading = rememberReading(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME)
    val hold = remember { HoldTracker() }
    var trace by remember { mutableStateOf(FloatArray(0)) }
    LaunchedEffect(Unit) { hold.reset(System.currentTimeMillis()) }

    val g = reading?.let { gForce(it[0], it[1], it[2]) }
    LaunchedEffect(reading) {
        val value = g ?: return@LaunchedEffect
        hold.update(value, System.currentTimeMillis())
        val kept = if (trace.size >= TRACE_LENGTH) trace.copyOfRange(1, trace.size) else trace
        trace = kept + value
    }

    Instrument(
        title = "G-force",
        onBack = onBack,
        footnote = if (reading == null) "waiting for the accelerometer" else null,
        actions = {
            Action("Reset peak") {
                hold.reset(System.currentTimeMillis())
                trace = FloatArray(0)
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(g?.let { String.format(Locale.US, "%.2f", it) } ?: "--", "g", "now")
            Reading(
                if (hold.peak > 0f) String.format(Locale.US, "%.2f", hold.peak) else "--",
                "g", "peak", style = Type.readingSmall,
            )
        }
        Micro(
            reading?.let {
                String.format(
                    Locale.US, "x %+.2f  y %+.2f  z %+.2f g",
                    it[0] / STANDARD_GRAVITY, it[1] / STANDARD_GRAVITY, it[2] / STANDARD_GRAVITY,
                )
            } ?: "x --  y --  z --",
            Modifier.padding(top = 8.dp),
            uppercase = false,
        )
        Trace(
            trace, 0f..3f,
            Modifier.fillMaxWidth().height(110.dp).padding(top = 18.dp),
        )
    }
}
