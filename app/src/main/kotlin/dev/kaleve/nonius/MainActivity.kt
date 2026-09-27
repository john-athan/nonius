package dev.kaleve.nonius

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.kaleve.nonius.ui.NoniusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NoniusTheme {
                Nonius()
            }
        }
    }

    // The keys reach the Activity before any composable does, so the Counter
    // is asked rather than told: it only has a handler registered while it is
    // the screen on show, and every other screen leaves both null, which
    // falls through to the ordinary volume change.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val handler = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> VolumeKeys.onUp
            KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeKeys.onDown
            else -> null
        }
        // Only the first press of a held key counts, never the repeats.
        if (handler != null && event.repeatCount == 0) {
            handler()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

/** Where the Counter parks its volume-key callbacks while it is on screen. */
object VolumeKeys {
    var onUp: (() -> Unit)? = null
    var onDown: (() -> Unit)? = null
}
