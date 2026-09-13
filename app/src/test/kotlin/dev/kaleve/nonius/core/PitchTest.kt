package dev.kaleve.nonius.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTest {

    @Test fun `concert pitch is A4 on the nose`() {
        val note = noteFor(440f)!!
        assertEquals("A4", note.label)
        assertEquals(0f, note.cents, 0.01f)
        assertTrue(note.inTune)
    }

    @Test fun `the octave rolls over at C and not at A`() {
        assertEquals("C4", noteFor(261.63f)!!.label)
        assertEquals("B4", noteFor(493.88f)!!.label)
        assertEquals("C5", noteFor(523.25f)!!.label)
        assertEquals("A3", noteFor(220f)!!.label)
        assertEquals("E2", noteFor(82.41f)!!.label)
    }

    @Test fun `a sharp string reads positive cents`() {
        val note = noteFor(444f)!!
        assertEquals("A4", note.label)
        assertEquals(15.7f, note.cents, 0.3f)
        assertTrue(!note.inTune)
    }

    @Test fun `baroque pitch moves every name with it`() {
        val note = noteFor(415f, a4 = 415f)!!
        assertEquals("A4", note.label)
        assertEquals(0f, note.cents, 0.01f)
    }

    @Test fun `nothing is not a note`() {
        assertNull(noteFor(0f))
        assertNull(noteFor(-3f))
    }
}
