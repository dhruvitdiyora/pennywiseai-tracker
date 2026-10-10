package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.presentation.common.AccountOption
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TransactionsMoreFiltersVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun unifiedSheetShowsEveryFilterFamily_light() {
        setContent(darkTheme = false) {
            sheet(onApply = { it.validation }, onDismiss = {})
        }

        listOf("Period", "Transaction type", "Category", "Profile", "Account", "Tag")
            .forEach { label ->
                composeTestRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            }
        composeTestRule.onNodeWithText("Amount range").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Original currency").performScrollTo().assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun unifiedSheetSupportsLargeFont_dark() {
        setContent(darkTheme = true, fontScale = 2f) {
            sheet(onApply = { it.validation }, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Profile").performScrollTo().assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun unifiedSheetPreservesTonalHierarchy_amoled() {
        setContent(darkTheme = true, amoled = true) {
            sheet(onApply = { it.validation }, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Filters").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun applyReturnsOneCompleteDraft() {
        var applied: TransactionFilterDraft? = null
        setContent(darkTheme = false) {
            sheet(
                onApply = {
                    applied = it
                    it.validation
                },
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Expense").performClick()
        // Every category starts ticked (#786); unticking Travel leaves only Food.
        composeTestRule.onNodeWithText("Travel").performClick()
        // The tag chips sit in a LazyRow at the end of the sheet's scrolling
        // column; performScrollTo only scrolls the closest scrollable (the row),
        // so scroll the column itself to its end.
        composeTestRule.onNode(hasScrollAction() and hasAnyDescendant(hasText("Tag")))
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        composeTestRule.onNodeWithText("work").performClick().assertIsSelected()
        composeTestRule.onNodeWithText("Apply").performClick()

        assertEquals(TransactionTypeFilter.EXPENSE, applied?.transactionType)
        assertEquals("Food", applied?.category)
        assertEquals("work", applied?.tag)
    }

    @Test
    fun resetChangesOnlyDraftUntilApply() {
        var applied: TransactionFilterDraft? = null
        var dismissals = 0
        setContent(darkTheme = false) {
            sheet(
                initialDraft = sampleDraft,
                onApply = {
                    applied = it
                    it.validation
                },
                onDismiss = { dismissals++ },
            )
        }

        composeTestRule.onNodeWithText("Reset").performClick()
        assertNull(applied)
        assertEquals(0, dismissals)

        composeTestRule.onNodeWithText("Apply").performClick()
        assertEquals(TransactionFilterDraft(), applied)
    }

    @Test
    fun cancelDoesNotApplyDraft() {
        var applied = false
        var dismissals = 0
        setContent(darkTheme = false) {
            sheet(
                onApply = {
                    applied = true
                    it.validation
                },
                onDismiss = { dismissals++ },
            )
        }

        composeTestRule.onNodeWithText("Expense").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(false, applied)
        assertEquals(1, dismissals)
    }

    @Test
    fun invalidAmountKeepsSheetOpenAndShowsInlineError() {
        setContent(darkTheme = false) {
            sheet(
                initialDraft = TransactionFilterDraft(
                    minimumText = "20",
                    maximumText = "10",
                ),
                onApply = { it.validation },
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Apply").performClick()

        composeTestRule
            .onNodeWithText("Minimum cannot be greater than maximum.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun nativeModeKeepsExistingCurrencySelectorCanonical() {
        setContent(darkTheme = false) {
            sheet(
                unifiedMode = false,
                onApply = { it.validation },
                onDismiss = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "Original-currency chips are available in unified mode. The existing currency selector remains active here."
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Composable
    private fun sheet(
        initialDraft: TransactionFilterDraft = sampleDraft,
        unifiedMode: Boolean = true,
        onApply: (TransactionFilterDraft) -> TransactionFilterDraftValidation,
        onDismiss: () -> Unit,
    ) {
        TransactionFiltersSheet(
            initialDraft = initialDraft,
            availableCategories = listOf("Food", "Travel"),
            profiles = listOf(
                ProfileEntity(1L, "Personal", "#B4637A"),
                ProfileEntity(2L, "Business", "#31748F"),
            ),
            accountOptions = listOf(AccountOption("Bank_1234", "Daily account")),
            availableTags = listOf("work", "holiday"),
            availableOriginalCurrencies = listOf("INR", "USD"),
            unifiedMode = unifiedMode,
            displayCurrency = "INR",
            onApply = onApply,
            onDismiss = onDismiss,
        )
    }

    private fun setContent(
        darkTheme: Boolean,
        fontScale: Float = 1f,
        amoled: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                isAmoledMode = amoled,
                blurEffects = false,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale)
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    private companion object {
        val sampleDraft = TransactionFilterDraft(
            period = TimePeriod.LAST_MONTH,
            transactionType = TransactionTypeFilter.INCOME,
            profileId = 1L,
            accountKey = "Bank_1234",
            minimumText = "10",
            maximumText = "500",
            originalCurrencies = setOf("USD"),
        )
    }
}
