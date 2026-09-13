package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test fun `an honest panel needs no correction`() {
        assertEquals(15.75f, pixelsPerMm(400f), 0.01f)
    }

    @Test fun `the correction scales the scale`() {
        assertEquals(16.54f, pixelsPerMm(400f, correction = 1.05f), 0.01f)
    }

    @Test fun `a card measured too short means the scale was too small`() {
        // The drawn card came out 82 mm where the real one is 85.6 mm.
        val correction = correctionFrom(drawnMm = 82f, realMm = CARD_LONG_MM)
        assertTrue(correction < 1f)
        assertTrue(correction in CORRECTION_RANGE)
    }
}
