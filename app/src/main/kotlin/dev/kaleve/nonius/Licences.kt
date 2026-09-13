package dev.kaleve.nonius

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.palette

/**
 * Apache 2.0 asks for its notice to travel with the binary, and an offline app
 * cannot send anyone to a web page to read it. So the text ships as a resource
 * and this is where it is read.
 */
const val LICENCES = "licences"

@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val text = remember {
        resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() }
    }
    Instrument(title = "Licences", onBack = onBack) {
        BasicText(
            text,
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp, color = palette.ink2),
        )
    }
}
