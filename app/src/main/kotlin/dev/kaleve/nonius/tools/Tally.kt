package dev.kaleve.nonius.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.palette
import dev.kaleve.nonius.ui.tapTarget
import kotlinx.coroutines.delay

@Composable
fun TallyScreen(onBack: () -> Unit) {
    // ponytail: no counting by volume key. The keys reach the Activity, not a
    // composable, so it needs a wire through MainActivity for this one tool.
    // Add it if counting without looking turns out to matter.
    var count by rememberSetting("tally.count", 0)
    var armed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // A reset that happens on one tap is a reset that happens by accident.
    LaunchedEffect(armed) {
        if (armed) {
            delay(3000)
            armed = false
        }
    }

    Instrument(
        title = "Counter",
        onBack = onBack,
        footnote = "Tap anywhere to count. The number survives being closed.",
        actions = {
            Action("Minus") {
                if (count > 0) {
                    count--
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            Action(if (armed) "Tap again" else "Reset", latched = armed) {
                if (armed) {
                    count = 0
                    armed = false
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    armed = true
                }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .tapTarget(onTap = {
                    count++
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                count.toString(),
                style = Type.reading.copy(
                    color = palette.ink,
                    fontSize = if (count >= 1000) 96.sp else 128.sp,
                ),
            )
        }
    }
}
