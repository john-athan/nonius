package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TiltTest {

    private val g = 9.81f

    @Test fun `face up on a table reads level`() {
        val tilt = Tilt.fromGravity(0f, 0f, g)
        assertEquals(0f, tilt.pitch, 0.001f)
        assertEquals(0f, tilt.roll, 0.001f)
        assertTrue(tilt.isLevel())
    }

    @Test fun `a raised right edge is a positive roll`() {
        // Tilted 30 degrees about the long axis: the reading picks up +g sin 30.
        val tilt = Tilt.fromGravity(g * 0.5f, 0f, g * 0.866f)
        assertEquals(30f, tilt.roll, 0.1f)
        assertEquals(0f, tilt.pitch, 0.1f)
        assertFalse(tilt.isLevel())
    }

    @Test fun `a raised top edge is a positive pitch`() {
        val tilt = Tilt.fromGravity(0f, g * 0.5f, g * 0.866f)
        assertEquals(30f, tilt.pitch, 0.1f)
    }

    @Test fun `standing upright is a quarter turn`() {
        assertEquals(90f, Tilt.fromGravity(0f, g, 0f).pitch, 0.1f)
    }

    @Test fun `zeroing subtracts the offset the device was built with`() {
        val zero = Tilt(0.6f, -0.4f)
        val corrected = Tilt(0.6f, -0.4f) - zero
        assertTrue(corrected.isLevel())
    }

    @Test fun `forty five degrees is a hundred percent`() {
        assertEquals("100.0", Slope.Percent.format(45f))
        assertEquals("1000", Slope.MmPerMetre.format(45f))
    }

    @Test fun `a rounded zero carries no minus sign`() {
        assertEquals("0.0", Slope.Degrees.format(-0.02f))
        assertEquals("-1.3", Slope.Degrees.format(-1.3f))
    }

    @Test fun `the scales cycle`() {
        assertEquals(Slope.Percent, Slope.Degrees.next())
        assertEquals(Slope.Degrees, Slope.MmPerMetre.next())
    }
}
