package dev.kaleve.nonius.tools

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kaleve.nonius.sensor.AudioFrame
import dev.kaleve.nonius.sensor.audioFrames
import kotlinx.coroutines.flow.emptyFlow

/**
 * Frames from the microphone, or nothing at all while the permission is not
 * given. Recording starts when a listening instrument is on screen and stops
 * when it leaves, which is the whole of the app's microphone policy.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberAudioFrame(granted: Boolean): AudioFrame? {
    val context = LocalContext.current
    val frames = remember(granted) { if (granted) context.audioFrames() else emptyFlow() }
    return frames.collectAsStateWithLifecycle(null).value
}
