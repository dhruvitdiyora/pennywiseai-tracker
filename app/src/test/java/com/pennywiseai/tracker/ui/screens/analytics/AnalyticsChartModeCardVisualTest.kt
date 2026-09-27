package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.components.BalancePoint
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AnalyticsChartModeCardVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun line_chart_card_light() {
        setContent(darkTheme = false, chartType = ChartType.LINE)

        composeTestRule.onNodeWithTag(CHART_CARD_TAG).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun heatmap_card_dark() {
        setContent(darkTheme = true, chartType = ChartType.HEATMAP)

        composeTestRule.onNodeWithTag(CHART_CARD_TAG).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun switching_modes_keeps_the_chart_container_visible() {
        val chartType = mutableStateOf(ChartType.LINE)
        composeTestRule.setContent {
            TestTheme(darkTheme = false) {
                AnalyticsChartModeCard(
                    chartType = chartType.value,
                    selectedCurrency = "USD",
                    data = chartData,
                    modifier = Modifier.testTag(CHART_CARD_TAG),
                )
            }
        }

        ChartType.entries.forEach { type ->
            composeTestRule.runOnIdle { chartType.value = type }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(CHART_CARD_TAG).assertIsDisplayed()
        }
    }

    private fun setContent(darkTheme: Boolean, chartType: ChartType) {
        composeTestRule.setContent {
            TestTheme(darkTheme = darkTheme) {
                AnalyticsChartModeCard(
                    chartType = chartType,
                    selectedCurrency = "USD",
                    data = chartData,
                    modifier = Modifier.testTag(CHART_CARD_TAG),
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TestTheme(
        darkTheme: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
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
                Box(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content()
                }
            }
        }
    }

    private companion object {
        const val CHART_CARD_TAG = "analytics_chart_card"

        val chartData = listOf(
            BalancePoint(LocalDateTime.of(2026, 8, 1, 12, 0), BigDecimal("125.50"), "USD"),
            BalancePoint(LocalDateTime.of(2026, 8, 8, 12, 0), BigDecimal("410.75"), "USD"),
            BalancePoint(LocalDateTime.of(2026, 8, 15, 12, 0), BigDecimal("285.25"), "USD"),
            BalancePoint(LocalDateTime.of(2026, 8, 22, 12, 0), BigDecimal("530.00"), "USD"),
        )
    }
}
