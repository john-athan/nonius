package dev.kaleve.nonius.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

val BPM_RANGE = 30f..250f
const val DEFAULT_BPM = 100f
val BEATS_PER_BAR_RANGE = 1f..12f
const val DEFAULT_BEATS_PER_BAR = 4f

/** The click itself: short, higher when it is beat one, silent at both edges. */
const val CLICK_DURATION_SECONDS = 0.02
const val ACCENT_HZ = 1000f
const val NORMAL_HZ = 800f

/** One beat's place on the sample counter, and its place in the bar. */
data class Click(val samplePosition: Long, val beatInBar: Int, val accented: Boolean)

/**
 * Where each beat falls on a running sample counter, never on a clock. A
 * beat's position is always the epoch plus one multiplication rounded once,
 * rather than a running total of already-rounded steps, so a beat interval
 * that is not a whole number of samples, which is almost every bpm, never
 * drifts no matter how long the metronome runs.
 *
 * A tempo or bar-length change takes its own epoch at the next beat
 * [clicksBefore] has not yet handed out, so every beat already due keeps its
 * old spacing and every beat after it uses the new one: no gap, no double
 * click at the seam.
 */
class ClickSchedule(private val sampleRate: Int, bpm: Float, beatsPerBar: Int) {
    private var bpm = bpm
    private var beatsPerBar = beatsPerBar
    private var epochSample = 0L
    private var epochBeat = 0L
    private var nextBeat = 0L

    private fun intervalSamples() = sampleRate * 60.0 / bpm

    private fun sampleOf(beat: Long) = epochSample + Math.round((beat - epochBeat) * intervalSamples())

    /** Re-anchors from the next beat not before [notBefore], the writer's own cursor. */
    fun setTempo(newBpm: Float, newBeatsPerBar: Int, notBefore: Long) {
        while (sampleOf(nextBeat) < notBefore) nextBeat++
        epochSample = sampleOf(nextBeat)
        epochBeat = nextBeat
        bpm = newBpm
        beatsPerBar = newBeatsPerBar
    }

    /** Every beat due before [endExclusive], in order, advancing past them. */
    fun clicksBefore(endExclusive: Long): List<Click> {
        val out = mutableListOf<Click>()
        while (true) {
            val position = sampleOf(nextBeat)
            if (position >= endExclusive) break
            val beatInBar = (nextBeat - epochBeat).mod(beatsPerBar.toLong()).toInt()
            out.add(Click(position, beatInBar, beatsPerBar > 1 && beatInBar == 0))
            nextBeat++
        }
        return out
    }
}

/**
 * One sample of the click waveform: a sine that has decayed to almost
 * nothing by the end of the burst, so it starts and stops without a click of
 * its own at either edge.
 */
fun clickSample(sampleIndex: Int, durationSamples: Int, accented: Boolean, sampleRate: Int): Float {
    val frequency = if (accented) ACCENT_HZ else NORMAL_HZ
    val envelope = exp(-6.0 * sampleIndex / durationSamples)
    return (sin(2.0 * PI * frequency * sampleIndex / sampleRate) * envelope * 0.8).toFloat()
}
