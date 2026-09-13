package dev.kaleve.nonius.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * One sensor event. Not a data class on purpose: the array is rewritten every
 * time, so value equality would be a lie and reference equality is exactly the
 * signal that a new reading arrived.
 */
class Reading(val values: FloatArray, val accuracy: Int) {
    operator fun get(index: Int): Float = values[index]

    /** Magnetometers drift and need the figure of eight wave. Nothing else does. */
    val unreliable: Boolean get() = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ||
        accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
}

/**
 * A sensor as a flow. Registering happens when someone collects and stops when
 * they walk away, so there is no lifecycle bookkeeping in any instrument and no
 * way to leave a sensor running in the background.
 */
fun Context.readings(type: Int, delayUs: Int = SensorManager.SENSOR_DELAY_GAME): Flow<Reading> =
    callbackFlow {
        val manager = getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(type)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Reading(event.values.copyOf(), event.accuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, delayUs)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

@Composable
fun rememberReading(type: Int, delayUs: Int = SensorManager.SENSOR_DELAY_GAME): Reading? {
    val context = LocalContext.current
    val flow = remember(type, delayUs) { context.readings(type, delayUs) }
    return flow.collectAsStateWithLifecycle(initialValue = null).value
}

fun Context.hasSensor(type: Int): Boolean =
    getSystemService(SensorManager::class.java)?.getDefaultSensor(type) != null
