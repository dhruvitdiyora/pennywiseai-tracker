package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.BudgetCategoryEntity
import com.pennywiseai.tracker.data.database.entity.BudgetEntity
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.database.entity.BudgetWithCategories
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionWithSplits
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.repository.BudgetCategorySpending
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class BudgetDetailVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun detailSummaryAndCategories_light() {
        setContent(darkTheme = false) {
            BudgetDetailContent(
                groupSpending = spending(),
                currency = "INR",
                unifiedMode = false,
                onTransactionClick = {},
            )
        }

        composeTestRule.onNodeWithText("ESSENTIALS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun detailTransactions_dark() {
        setContent(darkTheme = true) {
            BudgetDetailContent(
                groupSpending = spending(),
                currency = "INR",
                unifiedMode = false,
                onTransactionClick = {},
            )
        }

        composeTestRule.onNodeWithText("Matching transactions").performScrollTo()
        composeTestRule.onNodeWithText("Cafe").performScrollTo().assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun detailSupportsLargeFont_amoled() {
        setContent(darkTheme = true, amoled = true, fontScale = 2f) {
            BudgetDetailContent(
                groupSpending = spending(),
                currency = "INR",
                unifiedMode = false,
                onTransactionClick = {},
            )
        }

        composeTestRule.onNodeWithText("ESSENTIALS").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun transactionTapOpensExistingDetailCallback() {
        var selectedId = -1L
        setContent(darkTheme = false) {
            BudgetDetailContent(
                groupSpending = spending(),
                currency = "INR",
                unifiedMode = false,
                onTransactionClick = { selectedId = it },
            )
        }

        composeTestRule.onNodeWithText("Cafe").performScrollTo().performClick()

        assertEquals(102L, selectedId)
    }

    @Test
    fun emptyWindowExplainsMissingTransactions() {
        setContent(darkTheme = false) {
            BudgetDetailContent(
                groupSpending = spending().copy(matchingTransactions = emptyList()),
                currency = "INR",
                unifiedMode = false,
                onTransactionClick = {},
            )
        }

        composeTestRule
            .onNodeWithText("No transactions matched this budget during the selected window.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun spending(): BudgetGroupSpending {
        val start = LocalDate.of(2026, 9, 1)
        val budget = BudgetEntity(
            id = 42,
            name = "Essentials",
            limitAmount = BigDecimal("10000"),
            periodType = BudgetPeriodType.MONTHLY,
            startDate = start,
            endDate = start.plusMonths(1).minusDays(1),
            monthStartDay = 1,
        )
        val categories = listOf(
            BudgetCategoryEntity(
                id = 1,
                budgetId = 42,
                categoryName = "Groceries",
                budgetAmount = BigDecimal("6000"),
            ),
            BudgetCategoryEntity(
                id = 2,
                budgetId = 42,
                categoryName = "Dining",
                budgetAmount = BigDecimal("4000"),
            ),
        )
        return BudgetGroupSpending(
            group = BudgetWithCategories(budget, categories),
            categorySpending = listOf(
                BudgetCategorySpending(
                    categoryName = "Groceries",
                    budgetAmount = BigDecimal("6000"),
                    actualAmount = BigDecimal("3100"),
                    percentageUsed = 51.7f,
                    dailySpend = BigDecimal("103"),
                ),
                BudgetCategorySpending(
                    categoryName = "Dining",
                    budgetAmount = BigDecimal("4000"),
                    actualAmount = BigDecimal("2100"),
                    percentageUsed = 52.5f,
                    dailySpend = BigDecimal("70"),
                ),
            ),
            totalBudget = BigDecimal("10000"),
            totalActual = BigDecimal("5200"),
            remaining = BigDecimal("4800"),
            percentageUsed = 52f,
            dailyAllowance = BigDecimal("480"),
            daysRemaining = 10,
            daysElapsed = 20,
            windowStart = start,
            windowEnd = start.plusMonths(1).minusDays(1),
            windowDays = 30,
            periodType = BudgetPeriodType.MONTHLY,
            displayedCapDate = LocalDate.of(2026, 9, 20),
            displayedIsLive = true,
            matchingTransactions = listOf(
                transaction(101, "Supermarket", "Groceries", "3100"),
                transaction(102, "Cafe", "Dining", "2100"),
            ),
        )
    }

    private fun transaction(
        id: Long,
        merchant: String,
        category: String,
        amount: String,
    ) = TransactionWithSplits(
        transaction = TransactionEntity(
            id = id,
            amount = BigDecimal(amount),
            merchantName = merchant,
            category = category,
            transactionType = TransactionType.EXPENSE,
            dateTime = LocalDateTime.of(2026, 9, 12, 12, 30),
            transactionHash = "detail-$id",
        ),
        splits = emptyList(),
    )

    private fun setContent(
        darkTheme: Boolean,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale,
                ),
            ) {
                PennyWiseTheme(
                    darkTheme = darkTheme,
                    dynamicColor = false,
                    themeStyle = ThemeStyle.BRANDED,
                    isAmoledMode = amoled,
                    blurEffects = false,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
