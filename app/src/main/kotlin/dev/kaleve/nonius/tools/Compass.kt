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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val POINTS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/** Above this much swing away from the local field, there is iron in the way. */
private const val METAL_THRESHOLD_UT = 8f

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val rotation = rememberReading(Sensor.TYPE_ROTATION_VECTOR)
    val field = rememberReading(Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_UI)
    var held by remember { mutableStateOf<Float?>(null) }
    var metal by remember { mutableStateOf(false) }

    val heading = rotation?.let { headingOf(it.values) }
    val strength = field?.let { sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]) }

    // The rose turns the short way round and keeps turning past 360 rather than
    // spinning backwards through zero.
    var continuous by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(heading) {
        heading?.let { continuous += ((it - continuous) % 360f + 540f) % 360f - 180f }
    }
    val shown by animateFloatAsState(
        held ?: continuous, spring(1f, Spring.StiffnessMediumLow), label = "rose",
    )

    // The local field is whatever the room is made of. What matters for finding
    // a nail is the swing away from it, so the baseline is learned and forgotten.
    var baseline by remember { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(strength) {
        strength?.let { baseline = if (baseline.isNaN()) it else baseline * 0.97f + it * 0.03f }
    }
    val swing = if (strength == null || baseline.isNaN()) 0f else strength - baseline

    val bearing = ((shown % 360f) + 360f) % 360f
    Instrument(
        title = "Compass",
        onBack = onBack,
        trailing = strength?.let { String.format(Locale.US, "%.0f µT", it) },
        footnote = when {
            rotation == null -> "waiting for the compass"
            field?.unreliable == true -> "swing the phone through a figure of eight to calibrate"
            metal -> "hold the back of the phone flat against the wall and sweep"
            else -> "magnetic north, not true north"
        },
        actions = {
            Action("Hold", latched = held != null) { held = if (held == null) continuous else null }
            Action("Metal", latched = metal) { metal = !metal }
        },
    ) {
        Rose(
            bearing, if (metal) swing else null,
            Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 6.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Reading(
                bearing.roundToInt().toString(), "°",
                POINTS[((bearing / 45f).roundToInt()) % 8],
            )
            if (metal) {
                Reading(
                    String.format(Locale.US, "%+.1f", swing), "µT", "swing",
                    emphasis = abs(swing) > METAL_THRESHOLD_UT,
                    style = Type.readingSmall,
                )
            }
        }
    }
}

/**
 * Heading from the fused rotation vector rather than raw magnetometer readings,
 * because the fusion has already done the tilt compensation that makes a
 * compass usable in a hand that is not perfectly flat.
 */
private fun headingOf(vector: FloatArray): Float {
    // Some devices report five components; the platform helper only takes four.
    val trimmed = if (vector.size > 4) vector.copyOf(4) else vector
    val matrix = FloatArray(9)
    SensorManager.getRotationMatrixFromVector(matrix, trimmed)
    val orientation = FloatArray(3)
    SensorManager.getOrientation(matrix, orientation)
    return ((Math.toDegrees(orientation[0].toDouble()).toFloat() % 360f) + 360f) % 360f
}

@Composable
private fun Rose(bearing: Float, swing: Float?, modifier: Modifier) {
    val rule = palette.rule
    val ink = palette.ink
    val ink2 = palette.ink2
    val ink3 = palette.ink3
    val signal = palette.signal
    val measurer = rememberTextMeasurer()
    val cardinal = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val minor = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp)

    Canvas(modifier) {
        val centre = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - line(3f)

        // The lubber line stands still. Everything else turns under it.
        drawPath(
            Path().apply {
                moveTo(centre.x, centre.y - radius - line(1f))
                lineTo(centre.x - line(5f), centre.y - radius - line(11f))
                lineTo(centre.x + line(5f), centre.y - radius - line(11f))
                close()
            },
            signal,
        )

        rotate(-bearing, centre) {
            drawCircle(rule, radius, centre, style = hair())
            drawCircle(rule, radius * 0.9f, centre, style = hair())

            var angle = 0f
            while (angle < 360f) {
                val radians = Math.toRadians(angle.toDouble())
                val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
                val inner = when {
                    angle % 90f == 0f -> radius * 0.74f
                    angle % 30f == 0f -> radius * 0.78f
                    angle % 15f == 0f -> radius * 0.84f
                    else -> radius * 0.87f
                }
                drawLine(
                    if (angle % 90f == 0f) ink2 else ink3,
                    centre + Offset(direction.x * inner, direction.y * inner),
                    centre + Offset(direction.x * radius * 0.9f, direction.y * radius * 0.9f),
                    line(if (angle % 30f == 0f) 0.9f else 0.6f),
                )
                angle += 5f
            }

            POINTS.forEachIndexed { index, name ->
                if (index % 2 != 0) return@forEachIndexed
                val radians = Math.toRadians(index * 45.0)
                val text = measurer.measure(
                    name,
                    cardinal.copy(color = if (index == 0) signal else ink2),
                )
                val at = centre + Offset(
                    sin(radians).toFloat() * radius * 0.6f - text.size.width / 2f,
                    -cos(radians).toFloat() * radius * 0.6f - text.size.height / 2f,
                )
                drawText(text, topLeft = at)
            }
            listOf(30, 60, 120, 150, 210, 240, 300, 330).forEach { degrees ->
                val radians = Math.toRadians(degrees.toDouble())
                val text = measurer.measure(degrees.toString(), minor.copy(color = ink3))
                val at = centre + Offset(
                    sin(radians).toFloat() * radius * 0.6f - text.size.width / 2f,
                    -cos(radians).toFloat() * radius * 0.6f - text.size.height / 2f,
                )
                drawText(text, topLeft = at)
            }

            val needle = radius * 0.52f
            val waist = line(7f)
            drawPath(
                Path().apply {
                    moveTo(centre.x, centre.y - needle)
                    lineTo(centre.x + waist, centre.y)
                    lineTo(centre.x - waist, centre.y)
                    close()
                },
                signal,
            )
            drawPath(
                Path().apply {
                    moveTo(centre.x, centre.y + needle)
                    lineTo(centre.x + waist, centre.y)
                    lineTo(centre.x - waist, centre.y)
                    close()
                },
                ink3,
            )
            drawCircle(ink, line(3f), centre)
        }

        // Metal mode borrows the same face: a ring that thickens with the swing.
        if (swing != null) {
            val reach = (abs(swing) / 40f).coerceIn(0f, 1f)
            drawCircle(
                if (abs(swing) > METAL_THRESHOLD_UT) signal else ink3,
                radius * 0.97f,
                centre,
                style = hair(0.8f + 6f * reach),
                alpha = 0.25f + 0.55f * reach,
            )
        }
    }
}
