package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.repository.BudgetWindow
import com.pennywiseai.tracker.data.repository.PastWindowSpending
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class BudgetHistoryVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun trendAndCurrentWindow_light() {
        val windows = sampleWindows()
        setContent(darkTheme = false) {
            SpendingTrendChart(windows = windows, currency = "INR")
            HistoryRow(
                window = windows[1],
                currency = "INR",
                budgetAmount = BigDecimal("10000"),
                isDisplayed = true,
                isCurrentPeriod = true,
                onClick = {},
            )
        }

        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun overBudgetWindow_dark() {
        val window = pastWindow(
            start = LocalDate.of(2026, 8, 24),
            spent = "12500",
            isLive = false,
        )
        setContent(darkTheme = true) {
            HistoryRow(
                window = window,
                currency = "INR",
                budgetAmount = BigDecimal("10000"),
                isDisplayed = false,
                isCurrentPeriod = false,
                onClick = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun historyRowKeepsBreakdownCallback() {
        var clicks = 0
        setContent(darkTheme = false) {
            HistoryRow(
                window = sampleWindows().first(),
                currency = "INR",
                budgetAmount = BigDecimal("10000"),
                isDisplayed = false,
                isCurrentPeriod = false,
                onClick = { clicks++ },
            )
        }

        composeTestRule
            .onNodeWithText(CurrencyFormatter.formatCurrency(BigDecimal("4200"), "INR"))
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun percentageLabelIsBoundedForExtremeOverspend() {
        setContent(darkTheme = false) {
            HistoryRow(
                window = pastWindow(LocalDate.of(2026, 8, 3), "200000"),
                currency = "INR",
                budgetAmount = BigDecimal("10000"),
                isDisplayed = false,
                isCurrentPeriod = false,
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText("999%+").assertIsDisplayed()
    }

    @Test
    fun trendRequiresAtLeastTwoComparableWindows() {
        setContent(darkTheme = false) {
            SpendingTrendChart(
                windows = listOf(sampleWindows().first()),
                currency = "INR",
            )
        }

        composeTestRule.onNodeWithText("Spending trend").assertDoesNotExist()
    }

    private fun setContent(
        darkTheme: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        content()
                    }
                }
            }
        }
    }

    private fun sampleWindows() = listOf(
        pastWindow(LocalDate.of(2026, 8, 3), "4200"),
        pastWindow(LocalDate.of(2026, 8, 10), "7300", isLive = true),
        pastWindow(LocalDate.of(2026, 8, 17), "11200"),
    )

    private fun pastWindow(
        start: LocalDate,
        spent: String,
        isLive: Boolean = false,
    ) = PastWindowSpending(
        window = BudgetWindow(
            start = start,
            end = start.plusDays(6),
            days = 7,
        ),
        spent = BigDecimal(spent),
        capDate = if (isLive) start.plusDays(2) else start.plusDays(6),
        isLive = isLive,
    )
}
