package com.pennywiseai.tracker.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.RecurringFrequency
import com.pennywiseai.tracker.data.database.entity.RecurringTransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class RecurringTransactionsScreenVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun activeAndPausedTemplates_showEveryCadence_light() {
        setListContent(
            darkTheme = false,
            active = listOf(
                template("Daily groceries", RecurringFrequency.DAILY, TransactionType.EXPENSE, 1),
                template("Monthly salary", RecurringFrequency.MONTHLY, TransactionType.INCOME, 2),
                template("Weekly allowance", RecurringFrequency.WEEKLY, TransactionType.INCOME, 3),
            ),
            paused = listOf(
                template("Paused utility", RecurringFrequency.MONTHLY, TransactionType.EXPENSE, 4, isActive = false),
            ),
        )

        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paused utility").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly salary").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun longNamesAndPausedMenu_supportAmoledRtlAndLargeFont_dark() {
        setListContent(
            darkTheme = true,
            amoled = true,
            fontScale = 2f,
            layoutDirection = LayoutDirection.Rtl,
            active = listOf(
                template(
                    merchantName = "Northwind household maintenance and utilities reserve",
                    frequency = RecurringFrequency.MONTHLY,
                    transactionType = TransactionType.EXPENSE,
                    id = 10,
                ),
            ),
            paused = listOf(
                template("Paused weekly allowance", RecurringFrequency.WEEKLY, TransactionType.INCOME, 11, isActive = false),
            ),
        )

        composeTestRule.onNodeWithText("Northwind household maintenance and utilities reserve").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paused weekly allowance").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun itemMenu_pauseAndEdit_invokeCallbacks() {
        var toggledTo: Boolean? = null
        var edited: RecurringFormState? = null
        val item = template("Weekly allowance", RecurringFrequency.WEEKLY, TransactionType.INCOME, 20)
        setItemContent(
            item = item,
            onEdit = { edited = RecurringFormState.from(item) },
            onToggleActive = { toggledTo = it },
        )

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Pause").performClick()
        assertEquals(false, toggledTo)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        assertEquals(TransactionType.INCOME, edited?.transactionType)
        assertEquals(RecurringFrequency.WEEKLY, edited?.frequency)
    }

    @Test
    fun itemMenu_resumePausedTemplate_invokesCallback() {
        var resumedTo: Boolean? = null
        val item = template("Weekly allowance", RecurringFrequency.WEEKLY, TransactionType.INCOME, 20, isActive = false)
        setItemContent(
            item = item,
            onToggleActive = { resumedTo = it },
        )
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Resume").performClick()
        assertEquals(true, resumedTo)
    }

    @Test
    fun itemMenu_deleteConfirmation_supportsCancelAndConfirm() {
        var deleteCount = 0
        val item = template("Daily groceries", RecurringFrequency.DAILY, TransactionType.EXPENSE, 21)
        setItemContent(item = item, onDelete = { deleteCount++ })

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete recurring transaction?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete recurring transaction?").assertDoesNotExist()
        assertEquals(0, deleteCount)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete recurring transaction?").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Delete")[0].performClick()
        assertEquals(1, deleteCount)
    }

    @Test
    @Ignore(EDITOR_IGNORE_REASON)
    fun editor_invalidState_disablesSave_andMonthlyWeeklyDailyCadencesRender() {
        setEditorContent(RecurringFormState())

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Day of month (1–31)").assertIsDisplayed()

        composeTestRule.onNodeWithText("Monthly").performClick()
        composeTestRule.onNodeWithText("Daily").performClick()
        composeTestRule.onNodeWithText("Day of month (1–31)").assertDoesNotExist()

        composeTestRule.onNodeWithText("Daily").performClick()
        composeTestRule.onNodeWithText("Weekly").performClick()
        composeTestRule.onNodeWithText("Day of week").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Ignore(EDITOR_IGNORE_REASON)
    fun editor_validIncomeWeekly_saveCallback_preservesState() {
        var saved: RecurringFormState? = null
        setEditorContent(
            RecurringFormState(
                merchantName = "Monthly salary",
                amount = "12500",
                currency = "INR",
                category = "Salary",
                frequency = RecurringFrequency.MONTHLY,
                id = 22,
            ),
            onSave = { saved = it },
        )

        composeTestRule.onNodeWithText("Save").assertIsEnabled()
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.onNodeWithText("Monthly").performClick()
        composeTestRule.onNodeWithText("Weekly").performClick()
        composeTestRule.onNodeWithText("Day of week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(TransactionType.INCOME, saved?.transactionType)
        assertEquals(RecurringFrequency.WEEKLY, saved?.frequency)
        assertEquals(BigDecimal("12500"), saved?.amount?.toBigDecimalOrNull())
    }

    @Test
    @Ignore(EDITOR_IGNORE_REASON)
    fun editor_validState_supportsAmoledRtlLargeFont_dark() {
        setEditorContent(
            form = RecurringFormState(
                merchantName = "Northwind household maintenance and utilities reserve",
                amount = "825.50",
                currency = "USD",
                category = "Household",
                frequency = RecurringFrequency.MONTHLY,
                id = 23,
            ),
            darkTheme = true,
            amoled = true,
            fontScale = 2f,
            layoutDirection = LayoutDirection.Rtl,
        )

        composeTestRule.onNodeWithText("Edit recurring").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun recurringFormState_validation_requiresNamePositiveAmountAndCategory() {
        assertFalse(RecurringFormState().isValid)
        assertFalse(RecurringFormState(merchantName = "Daily groceries", amount = "0").isValid)
        assertFalse(RecurringFormState(merchantName = "Daily groceries", amount = "-1").isValid)
        assertTrue(
            RecurringFormState(
                merchantName = "Daily groceries",
                amount = "1.25",
                category = "Food",
            ).isValid,
        )
    }

    private fun setListContent(
        darkTheme: Boolean,
        active: List<RecurringTransactionEntity>,
        paused: List<RecurringTransactionEntity>,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            TestTheme(darkTheme, amoled, fontScale, layoutDirection) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (active.isNotEmpty()) {
                        SectionHeaderV2(title = "Active")
                        active.forEach { item ->
                            RecurringItem(
                                template = item,
                                onEdit = {},
                                onToggleActive = {},
                                onDelete = {},
                            )
                        }
                    }
                    if (paused.isNotEmpty()) {
                        SectionHeaderV2(title = "Paused")
                        paused.forEach { item ->
                            RecurringItem(
                                template = item,
                                onEdit = {},
                                onToggleActive = {},
                                onDelete = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setItemContent(
        item: RecurringTransactionEntity,
        onEdit: () -> Unit = {},
        onToggleActive: (Boolean) -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TestTheme {
                RecurringItem(
                    template = item,
                    onEdit = onEdit,
                    onToggleActive = onToggleActive,
                    onDelete = onDelete,
                )
            }
        }
    }

    private fun setEditorContent(
        form: RecurringFormState,
        darkTheme: Boolean = false,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onSave: (RecurringFormState) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TestTheme(darkTheme, amoled, fontScale, layoutDirection) {
                RecurringEditorDialog(
                    form = form,
                    categoryNames = listOf("Food", "Household", "Salary"),
                    onDismiss = {},
                    onSave = onSave,
                )
            }
        }
    }

    @Composable
    private fun TestTheme(
        darkTheme: Boolean = false,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        val baseDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density, fontScale),
            LocalLayoutDirection provides layoutDirection,
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

    private fun template(
        merchantName: String,
        frequency: RecurringFrequency,
        transactionType: TransactionType,
        id: Long,
        isActive: Boolean = true,
    ) = RecurringTransactionEntity(
        id = id,
        merchantName = merchantName,
        amount = if (transactionType == TransactionType.INCOME) BigDecimal("12500") else BigDecimal("825.50"),
        currency = "INR",
        category = if (transactionType == TransactionType.INCOME) "Salary" else "Household",
        transactionType = transactionType,
        frequency = frequency,
        dayOfMonth = if (frequency == RecurringFrequency.MONTHLY) 21 else null,
        dayOfWeek = if (frequency == RecurringFrequency.WEEKLY) 1 else null,
        nextDueDate = LocalDate.of(2026, 9, 21),
        isActive = isActive,
    )

    private companion object {
        // Material3's ExposedDropdownMenuBox updates state from a global-layout
        // listener; under Robolectric every layout pass re-fires it, so Compose
        // never reaches idle. Verify the editor dialog on a device instead.
        const val EDITOR_IGNORE_REASON =
            "ExposedDropdownMenuBox never idles under Robolectric; verify on device"
    }
}
