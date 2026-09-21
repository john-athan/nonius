package dev.kaleve.nonius.sensor

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

const val SAMPLE_RATE = 44100
const val FRAME_SIZE = 2048

/**
 * [samples] runs from -1 to 1. [unprocessed] says whether the device handed over
 * the raw microphone or the one the voice pipeline has already compressed and
 * gated, which decides whether a level reading means anything.
 */
class AudioFrame(val samples: FloatArray, val unprocessed: Boolean)

/**
 * Frames of microphone audio, about twenty a second.
 *
 * UNPROCESSED is asked for first: automatic gain control is exactly the thing a
 * sound level meter must not have, since it quietly rewrites the number being
 * measured. Where the device does not offer it, the plain microphone is used
 * and the instrument says so on screen rather than pretending.
 */
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
fun Context.audioFrames(sampleRate: Int = SAMPLE_RATE, frame: Int = FRAME_SIZE): Flow<AudioFrame> =
    flow {
        val unprocessed = getSystemService(AudioManager::class.java)
            ?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val source =
            if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
        val minimum = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        )
        // The microphone can be held by another app, missing behind a broken
        // HAL, or refuse this exact configuration, and the permission grant
        // rules out none of that. Any of it ends the flow the way an absent
        // sensor does, rather than crashing on a build() or startRecording()
        // that an unavailable device cannot serve.
        val record = try {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minimum, frame * 8))
                .build()
        } catch (unsupported: UnsupportedOperationException) {
            return@flow
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@flow
        }

        try {
            record.startRecording()
            val buffer = FloatArray(frame)
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < frame) {
                    val read = record.read(buffer, filled, frame - filled, AudioRecord.READ_BLOCKING)
                    if (read <= 0) return@flow
                    filled += read
                }
                emit(AudioFrame(buffer.copyOf(), unprocessed))
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

/** Whether the app may listen, and how to ask. */
class Microphone(val granted: Boolean, val ask: () -> Unit)

/**
 * The only permission prompt in the app, and it happens on opening one of the
 * two instruments that cannot work without it.
 */
@Composable
fun rememberMicrophone(): Microphone {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }
    return Microphone(granted) { launcher.launch(Manifest.permission.RECORD_AUDIO) }
}
