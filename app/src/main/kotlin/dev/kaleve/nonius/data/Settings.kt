package dev.kaleve.nonius.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * Everything the app remembers: a zero point per device, a screen correction, a
 * microphone offset, a concert pitch, a count. Six numbers, so the store is the
 * one the platform already has and there is no database in the build.
 */
private const val FILE = "nonius"

@Composable
private fun store(): SharedPreferences =
    LocalContext.current.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

/** A setting that reads like state and writes itself a moment after it settles. */
@Composable
fun rememberSetting(key: String, default: Float): MutableFloatState {
    val store = store()
    val state = remember(key) { mutableFloatStateOf(store.getFloat(key, default)) }
    LaunchedEffect(key) {
        snapshotFlow { state.value }.drop(1).collectLatest {
            delay(250)  // a drag settles once, rather than writing sixty times
            store.edit().putFloat(key, it).apply()
        }
    }
    return state
}

/** The counter writes at once: it has to survive the app being killed mid tap. */
@Composable
fun rememberSetting(key: String, default: Int): MutableIntState {
    val store = store()
    val state = remember(key) { mutableIntStateOf(store.getInt(key, default)) }
    LaunchedEffect(key) {
        snapshotFlow { state.value }.drop(1).collectLatest { store.edit().putInt(key, it).apply() }
    }
    return state
}
