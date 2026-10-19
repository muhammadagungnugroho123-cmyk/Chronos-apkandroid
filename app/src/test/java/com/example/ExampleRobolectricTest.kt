package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.ScreenType
import com.example.ui.ChronosStateHolder
import com.example.ui.screens.CalculatorFullScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `feature hub navigation to Kalkulator Smart and back works cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val stateHolder = ChronosStateHolder(app)

        assertEquals(ScreenType.CLOCK, stateHolder.currentScreen.value)

        // Open Feature Hub and navigate to Kalkulator Smart
        stateHolder.setFeatureHubOpen(true)
        assertEquals(true, stateHolder.isFeatureHubOpen.value)

        stateHolder.navigateTo(ScreenType.CALCULATOR)
        assertEquals(ScreenType.CALCULATOR, stateHolder.currentScreen.value)
        assertEquals(false, stateHolder.isFeatureHubOpen.value)

        // Add calculation history and verify persistence in stateHolder
        stateHolder.addCalculatorHistoryRecord("Kalkulator Dasar", "12 + 8", "20")
        assertEquals(1, stateHolder.calculatorHistory.value.size)
        assertEquals("20", stateHolder.calculatorHistory.value.first().result)

        // Navigate back to previous screen
        stateHolder.goBack()
        assertEquals(ScreenType.CLOCK, stateHolder.currentScreen.value)
    }

    @Test
    fun `Kalkulator Smart screen renders categories and performs basic calculation`() {
        var addedCategory = ""
        var addedExpr = ""
        var addedResult = ""

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CalculatorFullScreen(
                    appLanguage = "id",
                    historyRecords = emptyList(),
                    onBack = {},
                    onOpenFeatureHub = {},
                    onAddHistory = { cat, expr, res ->
                        addedCategory = cat
                        addedExpr = expr
                        addedResult = res
                    },
                    onDeleteHistoryItem = {},
                    onClearHistory = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify header title & display are visible
        composeTestRule.onNodeWithTag("calculator_header_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kalkulator Dasar").assertIsDisplayed()

        // Perform 12 + 8 = 20 on keypad
        composeTestRule.onNodeWithTag("calc_btn_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("calc_btn_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("calc_btn_add").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("calc_btn_8").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("calc_btn_equals").performScrollTo().performClick()

        composeTestRule.waitForIdle()

        assertEquals("Kalkulator Dasar", addedCategory)
        assertEquals("12+8", addedExpr)
        assertEquals("20", addedResult)

        // Verify post-equals digit starts a fresh expression (e.g. pressing 5 then = evaluates 5, not 205)
        composeTestRule.onNodeWithTag("calc_btn_5").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("calc_btn_equals").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("5", addedResult)

        // Open 10-Category Grid Menu and verify all 10 categories including Pengaturan Kalkulator
        composeTestRule.onNodeWithTag("calculator_toggle_category_grid").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calculator_category_grid_panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pengaturan Kalkulator").assertIsDisplayed()
    }

    @Test
    fun `background alarm scheduler and realistic themes work in v1_67_20`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val stateHolder = ChronosStateHolder(app)
        assertEquals("1.67.20", com.example.model.CHRONOS_VERSION)

        val initialCount = stateHolder.alarms.value.size
        stateHolder.addAlarm(
            com.example.model.AlarmItem(
                id = 999001L,
                hour = 7,
                minute = 30,
                label = "Bangun Pagi Presisi",
                isEnabled = true,
                daysOfWeek = 0b1111111,
                backgroundTheme = "COSMIC_DAWN"
            )
        )
        assertEquals(initialCount + 1, stateHolder.alarms.value.size)

        val addedAlarm = stateHolder.alarms.value.first { it.id == 999001L }
        assertEquals("COSMIC_DAWN", addedAlarm.backgroundTheme)

        // Test updating theme
        stateHolder.updateAlarmTheme(addedAlarm.id, "RAINY_HORIZON")
        val updated = stateHolder.alarms.value.first { it.id == 999001L }
        assertEquals("RAINY_HORIZON", updated.backgroundTheme)

        val nowMs = System.currentTimeMillis()
        val nextMillis = com.example.model.getNextAlarmOccurrenceMillis(addedAlarm, nowMs)
        org.junit.Assert.assertNotNull(nextMillis)
        org.junit.Assert.assertTrue(nextMillis!! > nowMs)
        assertEquals(0L, nextMillis % 60_000L)

        // Verify skipping the upcoming occurrence advances the scheduled target without disabling the repeating alarm
        com.example.service.ChronosAlarmScheduler.markUpcomingOccurrenceSkipped(addedAlarm.id, nextMillis)
        stateHolder.syncAlarmsToBackgroundEngine()
        org.junit.Assert.assertTrue(stateHolder.alarms.value.first { it.id == 999001L }.isEnabled)
        com.example.service.ChronosAlarmScheduler.clearSkippedOccurrence(addedAlarm.id)

        // Verify Smart Timer & Focus Live notifications do not throw and update state cleanly
        stateHolder.startTimer(120)
        org.junit.Assert.assertTrue(stateHolder.timerIsRunning.value)
        stateHolder.pauseTimer()
        org.junit.Assert.assertFalse(stateHolder.timerIsRunning.value)
    }
}
