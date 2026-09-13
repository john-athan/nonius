package dev.kaleve.nonius.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** The eight marks on the lids of the case. Drawn, so they cost nothing and scale. */
enum class Glyph { Vial, Rose, Rule, Wave, Fork, Counter, Barometer, Sun }

@Composable
fun ToolGlyph(glyph: Glyph, modifier: Modifier = Modifier, color: Color = palette.ink) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val weight = line(1.25f)
        val stroke = Stroke(width = weight, cap = StrokeCap.Round)
        fun at(x: Float, y: Float) = Offset(x * u, y * u)
        fun seg(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, at(x1, y1), at(x2, y2), weight, StrokeCap.Round)

        when (glyph) {
            Glyph.Vial -> {
                drawRoundRect(
                    color, at(2f, 9f), Size(20 * u, 8 * u),
                    CornerRadius(4 * u), style = stroke,
                )
                seg(10f, 9f, 10f, 17f)
                seg(14f, 9f, 14f, 17f)
                drawCircle(color, 2.4f * u, at(12f, 13f), style = stroke)
            }

            Glyph.Rose -> {
                drawCircle(color, 9.5f * u, at(12f, 12f), style = stroke)
                drawPath(
                    Path().apply {
                        moveTo(12 * u, 4.5f * u); lineTo(14 * u, 12 * u)
                        lineTo(12 * u, 19.5f * u); lineTo(10 * u, 12 * u); close()
                    },
                    color, style = stroke,
                )
            }

            Glyph.Rule -> {
                drawRect(color, at(3f, 7f), Size(18 * u, 10 * u), style = stroke)
                seg(7f, 7f, 7f, 11f); seg(11f, 7f, 11f, 13f)
                seg(15f, 7f, 15f, 11f); seg(19f, 7f, 19f, 13f)
            }

            Glyph.Wave -> {
                seg(4f, 10f, 4f, 14f); seg(8f, 7f, 8f, 17f); seg(12f, 4f, 12f, 20f)
                seg(16f, 7f, 16f, 17f); seg(20f, 10f, 20f, 14f)
            }

            Glyph.Fork -> {
                seg(9f, 4f, 9f, 12.5f); seg(15f, 4f, 15f, 12.5f)
                drawArc(
                    color, 0f, 180f, false, at(9f, 9.5f), Size(6 * u, 6 * u), style = stroke,
                )
                seg(12f, 15.5f, 12f, 20f)
            }

            Glyph.Counter -> {
                drawRoundRect(color, at(4f, 6f), Size(16 * u, 12 * u), CornerRadius(2 * u), style = stroke)
                seg(9f, 12f, 15f, 12f); seg(12f, 9f, 12f, 15f)
            }

            Glyph.Barometer -> {
                drawPath(
                    Path().apply {
                        moveTo(3 * u, 18 * u); lineTo(9 * u, 9 * u); lineTo(13 * u, 14 * u)
                        lineTo(16 * u, 10 * u); lineTo(21 * u, 18 * u)
                    },
                    color, style = stroke,
                )
                seg(3f, 20.5f, 21f, 20.5f)
            }

            Glyph.Sun -> {
                drawCircle(color, 4 * u, at(12f, 12f), style = stroke)
                seg(12f, 3f, 12f, 6f); seg(12f, 18f, 12f, 21f)
                seg(3f, 12f, 6f, 12f); seg(18f, 12f, 21f, 12f)
                seg(5.6f, 5.6f, 7.7f, 7.7f); seg(16.3f, 16.3f, 18.4f, 18.4f)
                seg(18.4f, 5.6f, 16.3f, 7.7f); seg(7.7f, 16.3f, 5.6f, 18.4f)
            }
        }
    }
}
