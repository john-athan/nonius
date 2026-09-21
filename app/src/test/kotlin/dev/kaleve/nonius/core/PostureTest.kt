package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostureTest {

    private val g = 9.81f

    @Test fun `a device lying flat has no posture`() {
        assertNull(postureFromGravity(0f, 0f, g))
    }

    @Test fun `a raised top edge is held portrait up`() {
        assertEquals(Posture.PortraitUp, postureFromGravity(0f, g, 0f))
    }

    @Test fun `a raised right edge is held landscape right`() {
        assertEquals(Posture.LandscapeRight, postureFromGravity(g, 0f, 0f))
    }

    @Test fun `a raised bottom edge is held portrait down`() {
        assertEquals(Posture.PortraitDown, postureFromGravity(0f, -g, 0f))
    }

    @Test fun `a raised left edge is held landscape left`() {
        assertEquals(Posture.LandscapeLeft, postureFromGravity(-g, 0f, 0f))
    }

    @Test fun `a device parked just past the neutral boundary keeps the current posture`() {
        // 46 degrees from landscape right, towards portrait up: past the neutral
        // 45 degree line but inside the wider band the current posture keeps.
        val radians = Math.toRadians(46.0)
        val x = (g * Math.cos(radians)).toFloat()
        val y = (g * Math.sin(radians)).toFloat()
        assertEquals(Posture.LandscapeRight, postureFromGravity(x, y, 0f, current = Posture.LandscapeRight))
    }

    @Test fun `a device well past the boundary switches even with hysteresis`() {
        val radians = Math.toRadians(70.0)
        val x = (g * Math.cos(radians)).toFloat()
        val y = (g * Math.sin(radians)).toFloat()
        assertEquals(Posture.PortraitUp, postureFromGravity(x, y, 0f, current = Posture.LandscapeRight))
    }

    @Test fun `the latch starts in portrait`() {
        assertEquals(Posture.PortraitUp, PostureLatch().posture)
    }

    @Test fun `a brief flick to landscape and back does not change the latched posture`() {
        val latch = PostureLatch()
        latch.update(Posture.LandscapeRight, 0L)
        latch.update(Posture.PortraitUp, 200L)
        assertEquals(Posture.PortraitUp, latch.posture)
    }

    @Test fun `a sustained turn changes the latched posture`() {
        val latch = PostureLatch()
        latch.update(Posture.LandscapeRight, 0L)
        val held = latch.update(Posture.LandscapeRight, 1300L)
        assertEquals(Posture.LandscapeRight, held)
    }

    @Test fun `a turn shorter than the dwell does not change the latched posture`() {
        val latch = PostureLatch()
        latch.update(Posture.LandscapeRight, 0L)
        val held = latch.update(Posture.LandscapeRight, 900L)
        assertEquals(Posture.PortraitUp, held)
    }
}
