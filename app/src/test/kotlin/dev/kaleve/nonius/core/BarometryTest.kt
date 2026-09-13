package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarometryTest {

    @Test fun `the reference point is zero`() {
        assertEquals(0f, altitudeMetres(1013.25f), 0.01f)
        assertEquals(0f, altitudeMetres(987.4f, referenceHpa = 987.4f), 0.01f)
    }

    @Test fun `one hectopascal is about eight metres near sea level`() {
        assertEquals(8.4f, altitudeMetres(1012.25f), 0.3f)
    }

    @Test fun `a storey of a house is measurable`() {
        // Roughly 0.37 hPa over three metres, which the sensor resolves.
        val storey = altitudeMetres(1012.88f)
        assertTrue("expected about three metres, got $storey", storey in 2.5f..3.5f)
    }

    @Test fun `less pressure is more height`() {
        assertTrue(altitudeMetres(900f) > altitudeMetres(1000f))
    }

    @Test fun `only a real change counts as weather`() {
        assertEquals("steady", pressureTrend(0.4f))
        assertEquals("falling", pressureTrend(-2f))
        assertEquals("rising", pressureTrend(1.5f))
    }
}
