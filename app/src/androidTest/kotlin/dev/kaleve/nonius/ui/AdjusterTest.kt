package dev.kaleve.nonius.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdjusterTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dragContinuesAfterValueRecompositionAndStartsFromResetValue() {
        val value = mutableFloatStateOf(50f)
        compose.setContent {
            Box(Modifier.width(300.dp).testTag("adjuster")) {
                Adjuster(value.floatValue, 0f..100f, "test", "${value.floatValue}",
                    onChange = { value.floatValue = it })
            }
        }
        val adjuster = compose.onNodeWithTag("adjuster")
        adjuster.performTouchInput {
            down(Offset(width * 0.1f, height - 1f))
            moveBy(Offset(width * 0.2f, 0f))
        }
        var afterFirstMove = 0f
        compose.runOnIdle {
            afterFirstMove = value.floatValue
            assertTrue("the initial drag must change the value", afterFirstMove > 50f)
        }
        // Keep the finger down while composition observes the new value.
        adjuster.performTouchInput {
            moveBy(Offset(width * 0.3f, 0f))
            up()
        }
        compose.runOnIdle {
            assertTrue("the same drag must keep changing the value",
                value.floatValue > afterFirstMove + 4f)
            value.floatValue = 80f
        }
        compose.waitForIdle()
        adjuster.performTouchInput {
            down(Offset(width * 0.7f, height - 1f))
            moveBy(Offset(-width * 0.3f, 0f))
            up()
        }
        compose.runOnIdle {
            assertTrue("a new drag must use the reset value",
                value.floatValue in 72f..79f)
        }
    }

    @Test fun smallMovementsAccumulateForIntegerRoundedCallers() {
        val value = mutableFloatStateOf(4f)
        compose.setContent {
            Box(Modifier.width(300.dp).testTag("adjuster")) {
                Adjuster(value.floatValue, 1f..12f, "beats per bar", "${value.floatValue}",
                    onChange = { value.floatValue = it.roundTo(1f) })
            }
        }
        val adjuster = compose.onNodeWithTag("adjuster")
        adjuster.performTouchInput {
            down(Offset(width * 0.1f, height - 1f))
            moveBy(Offset(width * 0.15f, 0f))
        }
        compose.runOnIdle { assertTrue(value.floatValue == 4f) }
        // Each move is below half a step; together they must change the setting.
        repeat(6) {
            adjuster.performTouchInput {
                repeat(10) { moveBy(Offset(width * 0.01f, 0f)) }
            }
            compose.waitForIdle()
        }
        adjuster.performTouchInput { up() }
        compose.runOnIdle {
            assertTrue("fractional movement must survive the caller's rounding",
                value.floatValue >= 5f)
        }
    }
}
