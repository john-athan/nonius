package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class GforceTest {

    @Test fun `lying still reads one g`() {
        assertEquals(1f, gForce(0f, 0f, STANDARD_GRAVITY), 0.001f)
    }

    @Test fun `magnitude combines all three axes`() {
        assertEquals(1.5f, gForce(0f, 0f, 1.5f * STANDARD_GRAVITY), 0.001f)
        val diagonal = STANDARD_GRAVITY / sqrt(2f)
        assertEquals(1f, gForce(diagonal, diagonal, 0f), 0.001f)
    }

    @Test fun `peak hold keeps the highest magnitude seen`() {
        val hold = HoldTracker()
        hold.reset(0L)
        hold.update(1.0f, 600L)
        hold.update(2.4f, 700L)
        hold.update(1.2f, 800L)
        assertEquals(2.4f, hold.peak, 0f)
    }
}
