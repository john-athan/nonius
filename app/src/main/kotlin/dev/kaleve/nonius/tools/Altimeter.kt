package dev.kaleve.nonius.tools

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kaleve.nonius.core.STANDARD_PRESSURE_HPA
import dev.kaleve.nonius.core.altitudeMetres
import dev.kaleve.nonius.core.pressureTrend
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.sensor.rememberReading
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.Trace
import dev.kaleve.nonius.ui.Type
import java.util.Locale

private const val TRACE_LENGTH = 180

@Composable
fun AltimeterScreen(onBack: () -> Unit) {
    val reading = rememberReading(Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_UI)
    var reference by rememberSetting("altimeter.reference", STANDARD_PRESSURE_HPA)
    var first by remember { mutableFloatStateOf(Float.NaN) }
    var opened by remember { mutableLongStateOf(0L) }
    var trace by remember { mutableStateOf(FloatArray(0)) }

    val pressure = reading?.get(0)
    LaunchedEffect(reading) {
        val now = pressure ?: return@LaunchedEffect
        if (first.isNaN()) {
            first = now
            opened = SystemClock.elapsedRealtime()
        }
        val kept = if (trace.size >= TRACE_LENGTH) trace.copyOfRange(1, trace.size) else trace
        trace = kept + now
    }

    val height = pressure?.let { altitudeMetres(it, reference) }
    val hours = (SystemClock.elapsedRealtime() - opened) / 3_600_000f
    val trend = when {
        pressure == null || first.isNaN() || hours < 0.08f -> "watching"
        else -> pressureTrend((pressure - first) / hours)
    }
    val relative = reference != STANDARD_PRESSURE_HPA

    Instrument(
        title = "Altimeter",
        onBack = onBack,
        trailing = if (relative) "from the mark" else "sea level",
        footnote = when {
            pressure == null -> "waiting for the barometer"
            relative -> "Height above the point where you set the mark."
            else -> "Height above sea level is only as right as today's weather."
        },
        actions = {
            Action("Set mark") { pressure?.let { reference = it } }
            Action("Sea level") { reference = STANDARD_PRESSURE_HPA }
        },
    ) {
        Reading(
            height?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
            "m",
            if (relative) "above the mark" else "above sea level",
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(
                pressure?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                "hPa", "pressure", style = Type.readingSmall,
            )
            Reading(trend, "", "trend", style = Type.readingSmall)
        }
        Trace(
            trace,
            (if (first.isNaN()) STANDARD_PRESSURE_HPA else first).let { it - 3f..it + 3f },
            Modifier.fillMaxWidth().height(96.dp).padding(top = 20.dp),
        )
    }
}
