package dev.kaleve.nonius.core

/** ISO 7810 ID-1, the bank card in everyone's pocket, to the hundredth. */
const val CARD_LONG_MM = 85.6f
const val CARD_SHORT_MM = 53.98f

const val MM_PER_INCH = 25.4f

/**
 * Pixels per millimetre down the screen.
 *
 * [reportedDpi] is the panel's own figure (DisplayMetrics.ydpi), which is the
 * physical one rather than the density bucket, and is right on most devices and
 * a few percent out on the rest. [correction] carries that difference and
 * nothing else, so 1.0 means the panel was honest.
 */
fun pixelsPerMm(reportedDpi: Float, correction: Float = 1f): Float =
    reportedDpi / MM_PER_INCH * correction

/** What the correction becomes after the user matches the drawn card to a real one. */
fun correctionFrom(drawnMm: Float, realMm: Float): Float = drawnMm / realMm

/** The range a panel report can plausibly be wrong by. Anything further is a slip. */
val CORRECTION_RANGE = 0.75f..1.35f
