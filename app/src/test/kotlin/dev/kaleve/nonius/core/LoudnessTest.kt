package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LoudnessTest {

    private fun sine(amplitude: Float, size: Int = 4410) =
        FloatArray(size) { amplitude * sin(2.0 * PI * 440.0 * it / 44100.0).toFloat() }

    @Test fun `a full scale sine sits three decibels below full scale`() {
        assertEquals(-3.01f, dbFullScale(rms(sine(1f))), 0.05f)
    }

    @Test fun `half the amplitude is six decibels less`() {
        val loud = dbFullScale(rms(sine(1f)))
        val quiet = dbFullScale(rms(sine(0.5f)))
        assertEquals(-6.02f, quiet - loud, 0.05f)
    }

    @Test fun `silence is floored rather than infinite`() {
        assertEquals(-140f, dbFullScale(rms(FloatArray(1024))), 0.1f)
    }

    @Test fun `the offset is the whole calibration`() {
        val level = soundPressureLevel(rms(sine(1f)), DEFAULT_SPL_OFFSET_DB)
        assertEquals(96.99f, level, 0.05f)
    }

    @Test fun `an empty frame does not divide by zero`() {
        assertEquals(0f, rms(FloatArray(0)), 0f)
    }
}
