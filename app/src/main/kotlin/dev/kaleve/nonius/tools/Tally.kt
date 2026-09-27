package dev.kaleve.nonius.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import dev.kaleve.nonius.VolumeKeys
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.palette
import kotlinx.coroutines.delay

@Composable
fun TallyScreen(onBack: () -> Unit) {
    var count by rememberSetting("tally.count", 0)
    var armed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Claim the volume keys for exactly as long as this screen is up; every
    // other screen leaves MainActivity's handlers null and the keys change
    // the volume as normal.
    DisposableEffect(Unit) {
        VolumeKeys.onUp = {
            count++
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        VolumeKeys.onDown = {
            if (count > 0) {
                count--
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        onDispose {
            VolumeKeys.onUp = null
            VolumeKeys.onDown = null
        }
    }

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
        footnote = "Tap anywhere, or use the volume keys, to count. The number survives being closed.",
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
                // Every finger that lands is one count, on the press, like a
                // mechanical tally. A tap detector follows one finger per
                // gesture and waits for all of them to lift, so drumming with
                // two or three fingers lost most of the taps.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val landed = awaitPointerEvent().changes.filter { it.changedToDown() }
                            if (landed.isEmpty()) continue
                            landed.forEach { it.consume() }
                            count += landed.size
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                },
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
