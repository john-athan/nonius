package dev.kaleve.nonius.tools

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.sensor.rememberReading
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.hair
import dev.kaleve.nonius.ui.line
import dev.kaleve.nonius.ui.palette
import java.util.Locale
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Six decades, from a moonlit room to a white wall in the sun. */
private const val DECADES = 6f
private const val FLOOR_LUX = 0.1f

/** What a number of lux feels like, which is the part a photographer wants. */
private fun sceneFor(lux: Float): String = when {
    lux < 1f -> "moonlight"
    lux < 20f -> "candlelit"
    lux < 80f -> "dim room"
    lux < 200f -> "living room"
    lux < 500f -> "good reading light"
    lux < 1000f -> "office, bright"
    lux < 5000f -> "overcast outside"
    lux < 25000f -> "daylight, shade"
    else -> "direct sun"
}

@Composable
fun LightScreen(onBack: () -> Unit) {
    val reading = rememberReading(Sensor.TYPE_LIGHT, SensorManager.SENSOR_DELAY_UI)
    var peak by remember { mutableFloatStateOf(0f) }
    val lux = reading?.get(0)
    LaunchedEffect(reading) { lux?.let { peak = max(peak, it) } }

    val decade = lux?.let { (log10(max(it, FLOOR_LUX)) + 1f) / DECADES } ?: 0f
    val sweep by animateFloatAsState(
        decade.coerceIn(0f, 1f), spring(1f, Spring.StiffnessLow), label = "arc",
    )

    Instrument(
        title = "Light meter",
        onBack = onBack,
        trailing = lux?.let { sceneFor(it) },
        footnote = when {
            lux == null -> "waiting for the light sensor"
            else -> "The sensor sits by the earpiece, so point that at what you are measuring."
        },
        actions = { Action("Reset peak") { peak = 0f } },
    ) {
        Gauge(sweep, Modifier.fillMaxWidth().aspectRatio(1.5f).padding(vertical = 6.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(lux?.let { format(it) } ?: "--", "lx", "illuminance")
            Reading(
                if (peak > 0f) format(peak) else "--", "lx", "peak", style = Type.readingSmall,
            )
        }
    }
}

private fun format(lux: Float) = when {
    lux < 10f -> String.format(Locale.US, "%.1f", lux)
    lux < 10000f -> lux.roundToInt().toString()
    else -> String.format(Locale.US, "%.0fk", lux / 1000f)
}

@Composable
private fun Gauge(fraction: Float, modifier: Modifier) {
    val rule = palette.rule
    val ink3 = palette.ink3
    val ink2 = palette.ink2
    val signal = palette.signal
    val measurer = rememberTextMeasurer()
    val numerals = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ink3)

    Canvas(modifier) {
        val pivot = Offset(size.width / 2, size.height * 0.92f)
        val radius = size.height * 0.8f
        val start = 180f
        val total = 180f

        drawArc(
            rule, start, total, false,
            Offset(pivot.x - radius, pivot.y - radius), Size(radius * 2, radius * 2),
            style = hair(),
        )
        // One mark per decade, labelled the way a lux table is.
        for (decade in 0..DECADES.toInt()) {
            val at = decade / DECADES
            val angle = Math.toRadians((start + total * at).toDouble())
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                ink2,
                pivot + Offset(direction.x * (radius - 14f * density), direction.y * (radius - 14f * density)),
                pivot + Offset(direction.x * radius, direction.y * radius),
                line(1f),
            )
            val label = measurer.measure(
                when (decade) {
                    0 -> "0.1"
                    1 -> "1"
                    2 -> "10"
                    3 -> "100"
                    4 -> "1k"
                    5 -> "10k"
                    else -> "100k"
                },
                numerals,
            )
            val text = pivot + Offset(
                direction.x * (radius - 30f * density) - label.size.width / 2f,
                direction.y * (radius - 30f * density) - label.size.height / 2f,
            )
            drawText(label, topLeft = text)
        }
        drawArc(
            signal, start, total * fraction, false,
            Offset(pivot.x - radius, pivot.y - radius), Size(radius * 2, radius * 2),
            style = hair(2.4f),
        )
        val angle = Math.toRadians((start + total * fraction).toDouble())
        val tip = pivot + Offset(
            cos(angle).toFloat() * radius * 0.9f, sin(angle).toFloat() * radius * 0.9f,
        )
        drawLine(signal, pivot, tip, line(1.4f))
        drawCircle(signal, line(3f), pivot)
    }
}
