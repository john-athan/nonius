package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetronomeTest {

    @Test fun `beats land exactly on n times the interval with no cumulative drift`() {
        val schedule = ClickSchedule(48000, 120f, 4)
        val clicks = schedule.clicksBefore(24000L * 10000)
        assertEquals(10000, clicks.size)
        clicks.forEachIndexed { n, click -> assertEquals(n * 24000L, click.samplePosition) }
    }

    @Test fun `a tempo change moves only the beats not yet handed out`() {
        val schedule = ClickSchedule(48000, 120f, 4)
        val before = schedule.clicksBefore(24000L * 3 + 1)
        assertEquals(listOf(0L, 24000L, 48000L, 72000L), before.map { it.samplePosition })

        // Doubling the tempo from the writer's cursor at sample 80000: the next
        // beat was already due at 96000, so it keeps that spot, and only the
        // ones after it fall at the new, tighter spacing.
        schedule.setTempo(240f, 4, 80000L)
        val after = schedule.clicksBefore(96000L + 12000L * 3 + 1)
        assertEquals(listOf(96000L, 108000L, 120000L, 132000L), after.map { it.samplePosition })
    }

    @Test fun `four beats per bar accents only the first`() {
        val schedule = ClickSchedule(48000, 120f, 4)
        val clicks = schedule.clicksBefore(24000L * 8)
        assertEquals(
            listOf(true, false, false, false, true, false, false, false),
            clicks.map { it.accented },
        )
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), clicks.map { it.beatInBar })
    }

    @Test fun `one beat per bar has no accent`() {
        val schedule = ClickSchedule(48000, 120f, 1)
        val clicks = schedule.clicksBefore(24000L * 4)
        assertTrue(clicks.none { it.accented })
    }

    @Test fun `the click decays towards silence by the end of the burst`() {
        // Energy near the start against energy near the end, rather than one
        // sample against another, since a single sine sample can sit on a zero
        // crossing regardless of the envelope around it.
        val duration = (48000 * CLICK_DURATION_SECONDS).toInt()
        fun energy(range: IntRange) =
            range.sumOf { clickSample(it, duration, accented = false, sampleRate = 48000).toDouble().let { s -> s * s } }
        val onset = energy(0 until duration / 10)
        val tail = energy(duration - duration / 10 until duration)
        assertTrue("the tail must be far quieter than the onset", tail < onset * 0.05)
    }
}
