package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

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

    // The IEC 61672 Class 1 A-weighting table, and the tolerance this test
    // holds the filter to at each frequency: tight through the band that
    // matters for everyday levels, looser at the top octave the standard
    // itself gives more room.
    private val aWeightingTableDb = mapOf(
        31.5 to -39.4, 63.0 to -26.2, 125.0 to -16.1, 250.0 to -8.6, 500.0 to -3.2,
        1000.0 to 0.0, 2000.0 to 1.2, 4000.0 to 1.0, 8000.0 to -1.1,
    )

    /**
     * Gain of the filter at one frequency, run long enough for the transient
     * from a cold start to die out and dropped before it is measured.
     */
    private fun weightedGainDb(hz: Double, sampleRate: Int): Double {
        val filter = AWeighting(sampleRate)
        val seconds = 2.0
        val settle = (0.5 * sampleRate).toInt()
        val n = (seconds * sampleRate).toInt()
        var sumIn = 0.0
        var sumOut = 0.0
        var counted = 0
        for (i in 0 until n) {
            val x = sin(2.0 * PI * hz * i / sampleRate)
            val y = filter.process(x.toFloat())
            if (i >= settle) {
                sumIn += x * x
                sumOut += y.toDouble() * y
                counted++
            }
        }
        val rmsIn = sqrt(sumIn / counted)
        val rmsOut = sqrt(sumOut / counted)
        return 20.0 * log10(rmsOut / rmsIn)
    }

    private fun toleranceDb(hz: Double) = if (hz >= 8000.0) 1.5 else 0.7

    @Test fun `A-weighting matches the IEC 61672 curve at 48 kHz`() {
        aWeightingTableDb.forEach { (hz, expected) ->
            val gain = weightedGainDb(hz, 48000)
            assertTrue(
                "at $hz Hz expected $expected +/- ${toleranceDb(hz)}, got $gain",
                Math.abs(gain - expected) <= toleranceDb(hz),
            )
        }
    }

    @Test fun `A-weighting matches the IEC 61672 curve at 44_1 kHz`() {
        aWeightingTableDb.forEach { (hz, expected) ->
            val gain = weightedGainDb(hz, 44100)
            assertTrue(
                "at $hz Hz expected $expected +/- ${toleranceDb(hz)}, got $gain",
                Math.abs(gain - expected) <= toleranceDb(hz),
            )
        }
    }

    @Test fun `the minimum ignores the first half second`() {
        val hold = HoldTracker()
        hold.reset(0L)
        hold.update(20f, 100L)
        assertTrue("a settling dip inside the first half second must not stick", hold.minimum.isNaN())
        hold.update(80f, 600L)
        assertEquals(80f, hold.minimum, 0f)
        hold.update(60f, 700L)
        assertEquals(60f, hold.minimum, 0f)
    }

    @Test fun `peak hold is not held back by the settling window`() {
        val hold = HoldTracker()
        hold.reset(0L)
        hold.update(90f, 100L)
        assertEquals(90f, hold.peak, 0f)
    }
}
