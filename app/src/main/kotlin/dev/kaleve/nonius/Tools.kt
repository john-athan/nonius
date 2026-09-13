package dev.kaleve.nonius

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.kaleve.nonius.sensor.hasSensor
import dev.kaleve.nonius.tools.AltimeterScreen
import dev.kaleve.nonius.tools.CompassScreen
import dev.kaleve.nonius.tools.LevelScreen
import dev.kaleve.nonius.tools.LightScreen
import dev.kaleve.nonius.tools.RulerScreen
import dev.kaleve.nonius.tools.SoundScreen
import dev.kaleve.nonius.tools.TallyScreen
import dev.kaleve.nonius.tools.TunerScreen
import dev.kaleve.nonius.ui.Glyph

/**
 * What a tool needs from the hardware. The case asks before it draws a lid, so
 * an instrument the device cannot support is never offered and never has to
 * apologise for itself on opening.
 */
sealed interface Needs {
    data object Nothing : Needs
    data class Sense(val type: Int, val absent: String) : Needs
    data object Hearing : Needs
}

fun Needs.metBy(context: Context): Boolean = when (this) {
    Needs.Nothing -> true
    is Needs.Sense -> context.hasSensor(type)
    Needs.Hearing -> context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
}

fun Needs.absentLabel(): String = when (this) {
    Needs.Nothing -> ""
    is Needs.Sense -> absent
    Needs.Hearing -> "no microphone"
}

@Immutable
class Tool(
    val id: String,
    val title: String,
    val measures: String,
    val glyph: Glyph,
    val needs: Needs,
    val screen: @Composable (onBack: () -> Unit) -> Unit,
)

/**
 * The whole app, in one list. A ninth instrument is a file and a line here; the
 * case, the navigation and the back gesture never learn its name.
 */
val Tools: List<Tool> = listOf(
    Tool(
        "level", "Level", "angle", Glyph.Vial,
        Needs.Sense(Sensor.TYPE_GRAVITY, "no motion sensor"),
    ) { LevelScreen(it) },
    Tool(
        "compass", "Compass", "heading, metal", Glyph.Rose,
        Needs.Sense(Sensor.TYPE_MAGNETIC_FIELD, "no compass"),
    ) { CompassScreen(it) },
    Tool("ruler", "Ruler", "millimetres", Glyph.Rule, Needs.Nothing) { RulerScreen(it) },
    Tool("sound", "Sound level", "decibels", Glyph.Wave, Needs.Hearing) { SoundScreen(it) },
    Tool("tuner", "Tuner", "pitch", Glyph.Fork, Needs.Hearing) { TunerScreen(it) },
    Tool("tally", "Counter", "tally", Glyph.Counter, Needs.Nothing) { TallyScreen(it) },
    Tool(
        "altimeter", "Altimeter", "height, weather", Glyph.Barometer,
        Needs.Sense(Sensor.TYPE_PRESSURE, "no barometer"),
    ) { AltimeterScreen(it) },
    Tool(
        "light", "Light meter", "lux", Glyph.Sun,
        Needs.Sense(Sensor.TYPE_LIGHT, "no light sensor"),
    ) { LightScreen(it) },
)
