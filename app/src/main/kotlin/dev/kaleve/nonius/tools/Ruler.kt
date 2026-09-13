package dev.kaleve.nonius.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.core.CARD_LONG_MM
import dev.kaleve.nonius.core.CARD_SHORT_MM
import dev.kaleve.nonius.core.CORRECTION_RANGE
import dev.kaleve.nonius.core.MM_PER_INCH
import dev.kaleve.nonius.core.pixelsPerMm
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Adjuster
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Reading
import dev.kaleve.nonius.ui.hair
import dev.kaleve.nonius.ui.line
import dev.kaleve.nonius.ui.palette
import java.util.Locale

@Composable
fun RulerScreen(onBack: () -> Unit) {
    // The panel's own figure, which is the physical one and not the density
    // bucket. Right on most devices, a few percent out on the rest, which is
    // what the correction is for.
    val reportedDpi = LocalResources.current.displayMetrics.ydpi
    var correction by rememberSetting("ruler.correction", 1f)
    var inches by rememberSaveable { mutableStateOf(false) }
    var calibrating by rememberSaveable { mutableStateOf(false) }
    var caliperMm by remember { mutableFloatStateOf(40f) }

    val perMm = pixelsPerMm(reportedDpi, correction)
    val value = if (inches) caliperMm / MM_PER_INCH else caliperMm

    Instrument(
        title = "Ruler",
        onBack = onBack,
        trailing = if (inches) "inch" else "mm",
        onTrailingClick = { inches = !inches },
        footnote = if (calibrating) {
            "Lay a bank card on the outline and adjust until the edges meet."
        } else {
            "Zero is the top of the scale. Drag the line to measure."
        },
        actions = {
            Action("Calibrate", latched = calibrating) { calibrating = !calibrating }
            Action("Reset") { caliperMm = 40f }
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(perMm) {
                    detectVerticalDragGestures { change, _ ->
                        caliperMm = (change.position.y / perMm).coerceAtLeast(0f)
                    }
                }
        ) {
            Scale(perMm, caliperMm, inches, calibrating, Modifier.fillMaxSize())
        }
        Reading(
            if (inches) String.format(Locale.US, "%.2f", value)
            else String.format(Locale.US, "%.1f", value),
            if (inches) "in" else "mm",
            "caliper",
            Modifier.padding(top = 10.dp),
        )
        if (calibrating) {
            Column(Modifier.padding(top = 14.dp)) {
                Adjuster(
                    value = correction,
                    range = CORRECTION_RANGE,
                    label = "screen correction",
                    display = String.format(Locale.US, "%.3f", correction),
                    onChange = { correction = it },
                    onReset = { correction = 1f },
                )
            }
        }
    }
}

@Composable
private fun Scale(
    perMm: Float,
    caliperMm: Float,
    inches: Boolean,
    calibrating: Boolean,
    modifier: Modifier,
) {
    val ink = palette.ink
    val ink2 = palette.ink2
    val ink3 = palette.ink3
    val rule2 = palette.rule2
    val signal = palette.signal
    val measurer = rememberTextMeasurer()
    val numerals = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ink2)

    Canvas(modifier) {
        val step = if (inches) perMm * MM_PER_INCH / 16f else perMm
        val major = if (inches) 16 else 10
        val middle = if (inches) 8 else 5
        val edge = line(1f)

        drawLine(ink2, Offset(edge, 0f), Offset(edge, size.height), line(1.2f))

        var index = 0
        while (index * step <= size.height) {
            val y = index * step
            val long = index % major == 0
            val medium = index % middle == 0
            val length = when {
                long -> 34f
                medium -> 22f
                else -> 13f
            } * density
            drawLine(
                if (long) ink else if (medium) ink2 else ink3,
                Offset(edge, y),
                Offset(edge + length, y),
                line(if (long) 1f else 0.7f),
            )
            if (long && index > 0) {
                val label = measurer.measure((index / major).toString(), numerals)
                drawText(label, topLeft = Offset(edge + length + 6f * density, y - label.size.height / 2f))
            }
            index++
        }

        // The card outline is the calibration: a real one laid on top either
        // matches the drawn edges or does not.
        if (calibrating) {
            val width = CARD_SHORT_MM * perMm
            val height = CARD_LONG_MM * perMm
            val left = size.width - width - 4f * density
            drawRect(
                rule2,
                Offset(left, size.height - height - 4f * density),
                Size(width, height),
                style = hair(1f),
            )
            val caption = measurer.measure(
                "85.60 x 53.98 mm",
                numerals.copy(color = ink3, fontSize = 10.sp),
            )
            drawText(
                caption,
                topLeft = Offset(left + 6f * density, size.height - height + 4f * density),
            )
        }

        val y = caliperMm * perMm
        if (y <= size.height) {
            drawLine(signal, Offset(0f, y), Offset(size.width, y), line(1.2f))
            val nib = 5f * density
            drawPath(
                Path().apply {
                    moveTo(size.width, y - nib)
                    lineTo(size.width - nib * 1.6f, y)
                    lineTo(size.width, y + nib)
                    close()
                },
                signal,
            )
        }
    }
}
