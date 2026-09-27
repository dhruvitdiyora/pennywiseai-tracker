package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, sdk = [35])
class BalanceCardTrendScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandedTrend_light() {
        setContent(darkTheme = false, approximate = false)

        composeTestRule.onNodeWithText("Balance trend").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 180 days").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun expandedTrendApproximate_dark() {
        setContent(darkTheme = true, approximate = true)

        composeTestRule.onNodeWithText("Approximate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Some historical balances could not be converted and are not included.")
            .assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun insufficientHistory_showsTruthfulEmptyState() {
        setContent(
            darkTheme = false,
            approximate = false,
            history = emptyList(),
        )

        composeTestRule.onNodeWithText(
            "Your trend will appear after balances are recorded on more than one day."
        ).assertIsDisplayed()
    }

    private fun setContent(
        darkTheme: Boolean,
        approximate: Boolean,
        history: List<BigDecimal> = listOf(
            BigDecimal("21000"),
            BigDecimal("22500"),
            BigDecimal("21800"),
            BigDecimal("24200"),
            BigDecimal("25100"),
            BigDecimal("26800"),
        ),
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BalanceCard(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        totalBalance = BigDecimal("26800"),
                        monthlyChange = BigDecimal("1200"),
                        monthlyChangePercent = 8,
                        currency = "INR",
                        currentMonthIncome = BigDecimal("48000"),
                        currentMonthExpenses = BigDecimal("17500"),
                        currentMonthTotal = BigDecimal("30500"),
                        balanceHistory = history,
                        spendingHistory = listOf(
                            BigDecimal("2000"),
                            BigDecimal("4800"),
                            BigDecimal("8200"),
                            BigDecimal("12100"),
                            BigDecimal("17500"),
                        ),
                        lastMonthSpending = BigDecimal("16300"),
                        availableCurrencies = listOf("INR"),
                        isBalanceHistoryApproximate = approximate,
                        isBalanceHidden = false,
                        accountBalances = listOf(account()),
                        onCurrencyClick = {},
                        onShowBreakdown = {},
                        initiallyExpanded = true,
                    )
                }
            }
        }
    }

    private fun account() = AccountBalanceEntity(
        bankName = "Primary Bank",
        accountLast4 = "2468",
        balance = BigDecimal("26800"),
        timestamp = LocalDateTime.of(2026, 9, 3, 12, 0),
        currency = "INR",
    )
}
