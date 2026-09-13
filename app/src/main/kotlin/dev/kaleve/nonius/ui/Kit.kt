package dev.kaleve.nonius.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Line weight for every face in the app, in device pixels. */
fun DrawScope.line(dp: Float = 0.7f): Float = dp * density

fun DrawScope.hair(dp: Float = 0.7f) = Stroke(width = line(dp))

/** One hairline across the layout. */
@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = palette.rule) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = line())
    }
}

@Composable
fun Micro(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = palette.ink3,
    uppercase: Boolean = true,
) = BasicText(
    // Labels are set in capitals; readings are not, because uppercasing a unit
    // turns the micro sign into a Greek capital and 49 uT into 49 MT.
    if (uppercase) text.uppercase() else text,
    modifier,
    Type.micro.copy(color = color),
)

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = palette.ink2) =
    BasicText(text, modifier, Type.body.copy(color = color))

/** A clickable with no ripple, because none of these surfaces are buttons. */
@Composable
fun Modifier.press(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)

/**
 * The frame every tool sits in: title, a rule, the instrument itself, and the
 * actions along the bottom where a thumb already is. Nothing else draws chrome,
 * which is why all eight screens feel like one case of tools.
 */
@Composable
fun Instrument(
    title: String,
    onBack: () -> Unit,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    footnote: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    // An instrument gets read while both hands are busy holding something else.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val chevron = palette.ink3
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.paper)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.press(onClick = onBack), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(18.dp)) {
                    val s = size.minDimension
                    val w = line(1.6f)
                    drawLine(chevron, Offset(s * 0.62f, s * 0.22f), Offset(s * 0.32f, s * 0.5f), w)
                    drawLine(chevron, Offset(s * 0.32f, s * 0.5f), Offset(s * 0.62f, s * 0.78f), w)
                }
                Spacer(Modifier.width(6.dp))
                BasicText(title, style = Type.title.copy(color = palette.ink))
            }
            if (trailing != null) {
                Box(
                    if (onTrailingClick == null) Modifier
                    else Modifier
                        .border(1.dp, palette.rule2, CircleShape)
                        .press(onClick = onTrailingClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Micro(
                        trailing,
                        color = if (onTrailingClick == null) palette.ink3 else palette.ink2,
                        uppercase = false,
                    )
                }
            }
        }
        Rule()
        Column(
            Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            verticalArrangement = Arrangement.Center,
        ) { content() }
        if (footnote != null) Body(footnote, Modifier.padding(bottom = 10.dp), palette.ink3)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { actions() }
    }
}

/** A pill that says what it does and shows when it is latched on. */
@Composable
fun RowScope.Action(label: String, latched: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(
        if (latched) palette.signal else if (pressed) palette.sink else Color.Transparent,
        label = "action fill",
    )
    Box(
        Modifier
            .weight(1f)
            .background(fill, CircleShape)
            .border(1.dp, if (latched) palette.signal else palette.rule2, CircleShape)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label.uppercase(),
            style = Type.micro.copy(
                color = if (latched) palette.paper else palette.ink2,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** A number, its unit, and what it is a number of. */
@Composable
fun Reading(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    style: TextStyle = Type.reading,
) {
    Column(modifier) {
        Micro(label)
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText(value, style = style.copy(color = if (emphasis) palette.level else palette.ink))
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                BasicText(
                    unit,
                    Modifier.padding(bottom = 7.dp),
                    Type.readingSmall.copy(color = palette.ink3, fontSize = style.fontSize * 0.4f),
                )
            }
        }
    }
}

/**
 * The one continuous control in the app: a knurled strip pushed sideways. A drag
 * across the full width moves the value by a fifth of its range, because every
 * use of this is a calibration and a calibration is never coarse.
 */
@Composable
fun Adjuster(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    display: String,
    onChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null,
) {
    val span = range.endInclusive - range.start
    val ruleColour = palette.rule2
    val signalColour = palette.signal
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Micro(label)
            Box(Modifier.press(enabled = onReset != null) { onReset?.invoke() }) {
                Micro(display, color = palette.ink2)
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(range, value) {
                    detectHorizontalDragGestures { _, delta ->
                        val step = delta / size.width * span * 0.2f
                        onChange((value + step).coerceIn(range.start, range.endInclusive))
                    }
                }
        ) {
            val middle = size.height / 2
            val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
            val spacing = 3f * density
            // Knurling that stands taller near the pointer, so the eye finds it
            // without a knob sitting on top of the scale.
            var x = 0f
            while (x <= size.width) {
                val distance = (x - fraction * size.width) / size.width
                val near = (1f - distance * distance * 6f).coerceIn(0f, 1f)
                val half = (3f + 5f * near) * density
                drawLine(ruleColour, Offset(x, middle - half), Offset(x, middle + half), line(0.6f))
                x += spacing
            }
            drawLine(
                signalColour,
                Offset(fraction * size.width, middle - 11f * density),
                Offset(fraction * size.width, middle + 11f * density),
                line(1.6f),
            )
        }
    }
}

/** Tap anywhere, for the tools whose whole face is the control. */
fun Modifier.tapTarget(onTap: () -> Unit, onLongPress: (() -> Unit)? = null) =
    pointerInput(onTap, onLongPress) {
        detectTapGestures(onTap = { onTap() }, onLongPress = onLongPress?.let { press -> { _: Offset -> press() } })
    }

fun Float.roundTo(step: Float) = (this / step).roundToInt() * step

/**
 * A short history, drawn as a filled trace. Used by the two instruments where
 * the last minute says more than the current value: the sound level and the
 * barometer.
 */
@Composable
fun Trace(values: FloatArray, range: ClosedFloatingPointRange<Float>, modifier: Modifier) {
    val ruleColour = palette.rule
    val signalColour = palette.signal
    val sinkColour = palette.sink
    Canvas(modifier) {
        drawRect(sinkColour, size = size)
        val span = range.endInclusive - range.start
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val y = size.height * fraction
            drawLine(ruleColour, Offset(0f, y), Offset(size.width, y), line(0.6f))
        }
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val y = size.height * (1f - ((value - range.start) / span).coerceIn(0f, 1f))
            if (index == 0) path.moveTo(0f, y) else path.lineTo(index * step, y)
        }
        drawPath(path, signalColour, style = hair(1.4f))
    }
}
