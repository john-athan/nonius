package dev.kaleve.nonius.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kaleve.nonius.core.BEATS_PER_BAR_RANGE
import dev.kaleve.nonius.core.BPM_RANGE
import dev.kaleve.nonius.core.CLICK_DURATION_SECONDS
import dev.kaleve.nonius.core.Click
import dev.kaleve.nonius.core.ClickSchedule
import dev.kaleve.nonius.core.DEFAULT_BEATS_PER_BAR
import dev.kaleve.nonius.core.DEFAULT_BPM
import dev.kaleve.nonius.core.clickSample
import dev.kaleve.nonius.data.rememberSetting
import dev.kaleve.nonius.ui.Action
import dev.kaleve.nonius.ui.Adjuster
import dev.kaleve.nonius.ui.Instrument
import dev.kaleve.nonius.ui.Micro
import dev.kaleve.nonius.ui.Type
import dev.kaleve.nonius.ui.palette
import dev.kaleve.nonius.ui.roundTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.Locale

private const val SAMPLE_RATE = 48000

/** Twenty milliseconds of samples: one write covers exactly one whole click. */
private const val CHUNK_FRAMES = (SAMPLE_RATE * CLICK_DURATION_SECONDS).toInt()

/**
 * One AudioTrack in streaming mode, fed from the same running sample counter
 * the schedule uses, never from a timer. A tempo change is only ever applied
 * from the writer's own cursor, so it can never touch a beat already written.
 */
private class MetronomeEngine(bpm: Float, beatsPerBar: Int) {
    private val schedule = ClickSchedule(SAMPLE_RATE, bpm, beatsPerBar)
    private var appliedBpm = bpm
    private var appliedBeatsPerBar = beatsPerBar
    private val clickLength = (SAMPLE_RATE * CLICK_DURATION_SECONDS).toInt()
    private val buffer = FloatArray(CHUNK_FRAMES)
    private var cursor = 0L
    private var pending: Click? = null

    // Kept only for the UI: the last few clicks actually written, so the beat
    // shown on screen can be read back off the track's own playback head
    // instead of off this cursor, which always runs ahead of what is audible.
    private var recent: List<Click> = emptyList()

    private val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(
            maxOf(
                AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT),
                CHUNK_FRAMES * 4 * 4,
            )
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    fun start() = track.play()

    /** Applies a pending tempo change, writes the next chunk, and reports the
     *  beat now sounding at the point the track has actually reached. */
    fun tick(bpm: Float, beatsPerBar: Int): Click? {
        if (bpm != appliedBpm || beatsPerBar != appliedBeatsPerBar) {
            schedule.setTempo(bpm, beatsPerBar, cursor)
            appliedBpm = bpm
            appliedBeatsPerBar = beatsPerBar
        }

        buffer.fill(0f)
        val end = cursor + CHUNK_FRAMES
        val due = schedule.clicksBefore(end)
        if (due.isNotEmpty()) recent = (recent + due).takeLast(8)
        val active = listOfNotNull(pending) + due
        pending = null
        for (click in active) {
            val clickEnd = click.samplePosition + clickLength
            val from = maxOf(click.samplePosition, cursor)
            val to = minOf(clickEnd, end)
            for (p in from until to) {
                val t = (p - click.samplePosition).toInt()
                buffer[(p - cursor).toInt()] += clickSample(t, clickLength, click.accented, SAMPLE_RATE)
            }
            if (clickEnd > end) pending = click
        }
        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        cursor = end

        val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        return recent.lastOrNull { it.samplePosition <= head }
    }

    fun release() {
        track.stop()
        track.release()
    }
}

/**
 * Beats as they sound, or nothing while stopped. A cold flow so leaving the
 * screen or the app going to the background, both of which end the
 * collector, always releases the same AudioTrack that opened it.
 */
private fun metronomeBeats(bpm: () -> Float, beatsPerBar: () -> Int): Flow<Click?> = flow {
    val engine = MetronomeEngine(bpm(), beatsPerBar())
    try {
        engine.start()
        while (currentCoroutineContext().isActive) emit(engine.tick(bpm(), beatsPerBar()))
    } finally {
        engine.release()
    }
}.flowOn(Dispatchers.IO).conflate()

@Composable
private fun rememberMetronomeBeat(playing: Boolean, bpm: Float, beatsPerBar: Int): Click? {
    val bpmNow = rememberUpdatedState(bpm)
    val beatsPerBarNow = rememberUpdatedState(beatsPerBar)
    val flow = remember(playing) {
        if (playing) metronomeBeats({ bpmNow.value }, { beatsPerBarNow.value }) else emptyFlow()
    }
    return flow.collectAsStateWithLifecycle(null).value
}

@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    var bpm by rememberSetting("metronome.bpm", DEFAULT_BPM)
    var beatsPerBarF by rememberSetting("metronome.beatsPerBar", DEFAULT_BEATS_PER_BAR)
    val beatsPerBar = beatsPerBarF.roundTo(1f).toInt().coerceIn(1, 12)
    var playing by remember { mutableStateOf(false) }
    var setting by rememberSaveable { mutableStateOf(false) }
    val beat = rememberMetronomeBeat(playing, bpm, beatsPerBar)

    Instrument(
        title = "Metronome",
        onBack = onBack,
        trailing = String.format(Locale.US, "%.0f bpm", bpm),
        onTrailingClick = { setting = !setting },
        footnote = if (!playing) "Start to hear the beat. It stops when you leave or the app goes to the background." else null,
        actions = {
            Action(if (playing) "Stop" else "Start", latched = playing) { playing = !playing }
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                beat?.let { (it.beatInBar + 1).toString() } ?: "-",
                style = Type.reading.copy(
                    color = if (beat?.accented == true) palette.signal else palette.ink,
                    fontSize = 128.sp,
                ),
            )
            Micro(
                "beat ${(beat?.beatInBar ?: 0) + 1} of $beatsPerBar",
                Modifier.padding(top = 8.dp),
            )
        }
        if (setting) {
            Spacer(Modifier.height(28.dp))
            Adjuster(
                value = bpm,
                range = BPM_RANGE,
                label = "tempo",
                display = String.format(Locale.US, "%.0f bpm", bpm),
                onChange = { bpm = it.roundTo(1f) },
                onReset = { bpm = DEFAULT_BPM },
            )
            Spacer(Modifier.height(14.dp))
            Adjuster(
                value = beatsPerBarF,
                range = BEATS_PER_BAR_RANGE,
                label = "beats per bar",
                display = "$beatsPerBar",
                onChange = { beatsPerBarF = it.roundTo(1f) },
                onReset = { beatsPerBarF = DEFAULT_BEATS_PER_BAR },
            )
        }
    }
}
