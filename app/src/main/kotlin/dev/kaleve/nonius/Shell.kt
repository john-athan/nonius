package dev.kaleve.nonius

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.kaleve.nonius.ui.Body
import dev.kaleve.nonius.ui.Micro
import dev.kaleve.nonius.ui.Rule
import dev.kaleve.nonius.ui.ToolGlyph
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.palette
import dev.kaleve.nonius.ui.press
import kotlin.coroutines.cancellation.CancellationException

/**
 * Case and instrument, and nothing between them. The open tool is a string so
 * that the state survives a rotation without a navigation library, a graph, or
 * a route to keep in step with the list of tools.
 */
@Composable
fun Nonius() {
    val context = LocalContext.current
    val tools = remember { Tools.sortedBy { !it.needs.metBy(context) } }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var retreat by remember { mutableFloatStateOf(0f) }

    // The back gesture drags the instrument back into the case rather than
    // cutting to it, and letting go halfway leaves it open.
    PredictiveBackHandler(open != null) { gesture ->
        try {
            gesture.collect { retreat = it.progress }
            open = null
        } catch (cancelled: CancellationException) {
            // The user changed their mind; the instrument springs back.
        } finally {
            retreat = 0f
        }
    }

    AnimatedContent(
        targetState = open,
        transitionSpec = {
            if (targetState == null) {
                (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 1.04f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(220), targetScale = 0.94f))
            } else {
                (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(220), targetScale = 1.03f))
            }
        },
        label = "case",
    ) { id ->
        val tool = tools.firstOrNull { it.id == id }
        if (id == LICENCES) {
            LicencesScreen { open = null }
        } else if (tool == null) {
            Case(tools, onLicences = { open = LICENCES }) { open = it.id }
        } else {
            Box(
                Modifier.graphicsLayer {
                    val shrink = 1f - 0.06f * retreat
                    scaleX = shrink
                    scaleY = shrink
                    alpha = 1f - 0.25f * retreat
                }
            ) { tool.screen { open = null } }
        }
    }
}

@Composable
private fun Case(tools: List<Tool>, onLicences: () -> Unit, onOpen: (Tool) -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.paper)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText("Nonius", style = Type.wordmark.copy(color = palette.ink))
            Micro("no network", Modifier.padding(bottom = 4.dp))
        }
        Body(
            "${tools.count { it.needs.metBy(context) }} instruments, offline",
            Modifier.padding(bottom = 16.dp),
            palette.ink3,
        )
        Rule(color = palette.rule2)
        // The grid is the screen. Rows share the height that is left, so the
        // case never ends in a half page of nothing.
        Column(Modifier.weight(1f)) {
            tools.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Lid(pair[0], Modifier.weight(1f), onOpen)
                    Column(Modifier.fillMaxHeight().width(1.dp).background(palette.rule)) {}
                    if (pair.size > 1) Lid(pair[1], Modifier.weight(1f), onOpen)
                    else Spacer(Modifier.weight(1f))
                }
                Rule()
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Micro("GPL-3.0 or later")
            Box(Modifier.press(onClick = onLicences)) { Micro("licences", color = palette.ink2) }
        }
    }
}

@Composable
private fun Lid(tool: Tool, modifier: Modifier, onOpen: (Tool) -> Unit) {
    val available = tool.needs.metBy(LocalContext.current)
    Column(
        modifier
            .fillMaxHeight()
            .press(enabled = available) { onOpen(tool) }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        ToolGlyph(
            tool.glyph,
            Modifier.size(24.dp),
            color = if (available) palette.ink else palette.rule2,
        )
        Spacer(Modifier.height(10.dp))
        BasicText(
            tool.title,
            style = Type.body.copy(
                color = if (available) palette.ink else palette.ink3,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ),
        )
        Micro(if (available) tool.measures else tool.needs.absentLabel())
    }
}
