package dev.kaleve.nonius.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Paper, graphite, one signal colour, and a green that only ever means the
 * instrument has found its mark. Eleven values, and every surface in the app is
 * made of them.
 */
@Immutable
data class Palette(
    val paper: Color,
    val plate: Color,
    val sink: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val rule: Color,
    val rule2: Color,
    val signal: Color,
    val level: Color,
    val glass: Color,
)

private val Daylight = Palette(
    paper = Color(0xFFF8F5EF),
    plate = Color(0xFFEEEAE2),
    sink = Color(0xFFE2DDD4),
    ink = Color(0xFF241E1A),
    ink2 = Color(0xFF5D5751),
    ink3 = Color(0xFF8B8580),
    rule = Color(0xFFCFCAC1),
    rule2 = Color(0xFFB2AA9D),
    signal = Color(0xFFC45400),
    level = Color(0xFF257C3A),
    glass = Color(0xFFC2DEDF),
)

private val Lamplight = Palette(
    paper = Color(0xFF14100B),
    plate = Color(0xFF1E1812),
    sink = Color(0xFF0C0805),
    ink = Color(0xFFEBE7DF),
    ink2 = Color(0xFFA9A49C),
    ink3 = Color(0xFF78746D),
    rule = Color(0xFF332D25),
    rule2 = Color(0xFF4E463C),
    signal = Color(0xFFF4993C),
    level = Color(0xFF53BE70),
    glass = Color(0xFF3A5C60),
)

val LocalPalette = staticCompositionLocalOf { Daylight }

val palette: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

@Composable
fun NoniusTheme(night: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (night) Lamplight else Daylight, content = content)
}

/**
 * Four styles. Readings are monospaced with fixed width digits, because a
 * number that shifts sideways while it counts is unreadable on a moving
 * instrument, and nothing here is worth shipping a font file for.
 */
object Type {
    val wordmark = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val micro = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.4.sp,
        fontWeight = FontWeight.Medium,
    )
    val reading = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = 44.sp, letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum",
    )
    val readingSmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontFeatureSettings = "tnum",
    )
}
