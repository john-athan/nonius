package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class YinTest {

    private val rate = 44100

    private fun tone(vararg partials: Pair<Float, Float>, size: Int = 2048) =
        FloatArray(size) { i ->
            partials.sumOf { (hz, amplitude) ->
                amplitude * sin(2.0 * PI * hz * i / rate)
            }.toFloat()
        }

    @Test fun `concert A comes back as concert A`() {
        assertEquals(440f, detectPitchHz(tone(440f to 1f), rate)!!, 1f)
    }

    @Test fun `the low E string of a guitar is inside the range`() {
        assertEquals(82.41f, detectPitchHz(tone(82.41f to 1f), rate)!!, 1f)
    }

    @Test fun `a weak fundamental is still the fundamental`() {
        // What a plucked string actually looks like: the octave is the loudest
        // partial. A peak picker answers 392 here. YIN has to answer 196.
        val string = tone(196f to 0.3f, 392f to 1f, 588f to 0.6f)
        assertEquals(196f, detectPitchHz(string, rate)!!, 1.5f)
    }

    @Test fun `silence is not a pitch`() {
        assertNull(detectPitchHz(FloatArray(2048), rate))
    }

    @Test fun `noise is not a pitch`() {
        val random = Random(7)
        val noise = FloatArray(2048) { random.nextFloat() * 2f - 1f }
        assertNull(detectPitchHz(noise, rate))
    }

    @Test fun `a frame too short to hold two periods is refused`() {
        assertNull(detectPitchHz(FloatArray(8), rate))
    }

    @Test fun `the top of a violin still resolves`() {
        assertNotNull(detectPitchHz(tone(1568f to 1f), rate, maxHz = 1600f))
    }
}
