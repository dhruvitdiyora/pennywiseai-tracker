package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.BudgetEntity
import com.pennywiseai.tracker.data.database.entity.BudgetPeriodType
import com.pennywiseai.tracker.data.database.entity.BudgetWithCategories
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.repository.BudgetCategorySpending
import com.pennywiseai.tracker.data.repository.BudgetGroupSpending
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
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
class BudgetGroupsOverviewCardVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun activeBudgetCollapsed_light() {
        setCard(darkTheme = false, spending = spending())

        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Show breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("View details").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("More actions for Essentials")
            .assertHeightIsAtLeast(Dimensions.Component.minTouchTarget)
            .assertWidthIsAtLeast(Dimensions.Component.minTouchTarget)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun overBudgetExpanded_dark() {
        setCard(
            darkTheme = true,
            spending = spending(
                actual = "12500",
                remaining = "-2500",
                percentageUsed = 125f,
            ),
        )

        composeTestRule.onNodeWithText("Show breakdown").performClick()
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hide breakdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun overflowActionsKeepTheirCallbacksAndDisabledState() {
        var editCalls = 0
        var historyCalls = 0
        var moveUpCalls = 0
        var moveDownCalls = 0
        var deleteCalls = 0
        var detailCalls = 0

        setCard(
            darkTheme = false,
            spending = spending(),
            isFirst = true,
            isLast = false,
            onEdit = { editCalls++ },
            onViewHistory = { historyCalls++ },
            onMoveUp = { moveUpCalls++ },
            onMoveDown = { moveDownCalls++ },
            onDelete = { deleteCalls++ },
            onViewDetails = { detailCalls++ },
        )

        composeTestRule.onNodeWithText("View details").performClick()

        openMenu()
        composeTestRule.onNodeWithText("Move up").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Edit").performClick()

        openMenu()
        composeTestRule.onNodeWithText("View this period history").performClick()

        openMenu()
        composeTestRule.onNodeWithText("Move down").performClick()

        openMenu()
        composeTestRule.onNodeWithText("Delete").performClick()

        assertEquals(1, editCalls)
        assertEquals(1, historyCalls)
        assertEquals(0, moveUpCalls)
        assertEquals(1, moveDownCalls)
        assertEquals(1, deleteCalls)
        assertEquals(1, detailCalls)
    }

    private fun openMenu() {
        composeTestRule
            .onNodeWithContentDescription("More actions for Essentials")
            .performClick()
    }

    private fun setCard(
        darkTheme: Boolean,
        spending: BudgetGroupSpending,
        isFirst: Boolean = false,
        isLast: Boolean = false,
        onEdit: () -> Unit = {},
        onDelete: () -> Unit = {},
        onMoveUp: () -> Unit = {},
        onMoveDown: () -> Unit = {},
        onViewHistory: () -> Unit = {},
        onViewDetails: () -> Unit = {},
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
                    ) {
                        BudgetOverviewCard(
                            groupSpending = spending,
                            currency = "INR",
                            isFirst = isFirst,
                            isLast = isLast,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onMoveUp = onMoveUp,
                            onMoveDown = onMoveDown,
                            onViewHistory = onViewHistory,
                            onViewDetails = onViewDetails,
                            onCategoryClick = {},
                        )
                    }
                }
            }
        }
    }

    private fun spending(
        actual: String = "4200",
        remaining: String = "5800",
        percentageUsed: Float = 42f,
    ): BudgetGroupSpending {
        val windowStart = LocalDate.of(2026, 9, 1)
        val budget = BudgetEntity(
            id = 42,
            name = "Essentials",
            limitAmount = BigDecimal("10000"),
            periodType = BudgetPeriodType.MONTHLY,
            startDate = windowStart,
            endDate = windowStart.plusMonths(1).minusDays(1),
            monthStartDay = 1,
        )
        return BudgetGroupSpending(
            group = BudgetWithCategories(budget = budget, categories = emptyList()),
            categorySpending = listOf(
                BudgetCategorySpending(
                    categoryName = "Groceries",
                    budgetAmount = BigDecimal("6000"),
                    actualAmount = BigDecimal(actual).min(BigDecimal("6000")),
                    percentageUsed = percentageUsed.coerceAtMost(100f),
                    dailySpend = BigDecimal("140"),
                ),
            ),
            totalBudget = BigDecimal("10000"),
            totalActual = BigDecimal(actual),
            remaining = BigDecimal(remaining),
            percentageUsed = percentageUsed,
            dailyAllowance = BigDecimal("193.33"),
            daysRemaining = 30,
            daysElapsed = 1,
            dailyCumulativeSpending = listOf(1200.0, 2600.0, 4200.0),
            dailyBudgetPace = listOf(1000.0, 2000.0, 3000.0),
            windowStart = windowStart,
            windowEnd = windowStart.plusMonths(1).minusDays(1),
            windowDays = 30,
            periodType = BudgetPeriodType.MONTHLY,
        )
    }
}
