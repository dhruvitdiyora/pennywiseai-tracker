package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
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
class LendBorrowEntryFormVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lentEntry_light() {
        setContent(darkTheme = false) {
            LendBorrowEntryForm(
                initialDraft = LoanEntryDraft(
                    personName = "Aarav",
                    direction = LoanDirection.LENT,
                    amountText = "1250.50",
                    currency = "INR",
                    note = "Train tickets",
                ),
                error = null,
                isSaving = false,
                onInputChanged = {},
                onSubmit = { _, _, _, _, _ -> },
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun borrowedEntryWithError_dark() {
        setContent(darkTheme = true) {
            LendBorrowEntryForm(
                initialDraft = LoanEntryDraft(
                    personName = "Mira",
                    direction = LoanDirection.BORROWED,
                    amountText = "80",
                    currency = "USD",
                ),
                error = LoanEntryError.SAVE_FAILED,
                isSaving = false,
                onInputChanged = {},
                onSubmit = { _, _, _, _, _ -> },
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun validDraftSubmitsExactCurrencyTaggedValues() {
        var submittedName: String? = null
        var submittedDirection: LoanDirection? = null
        var submittedAmount: BigDecimal? = null
        var submittedCurrency: String? = null
        var submittedNote: String? = "not-called"
        setContent(darkTheme = false) {
            LendBorrowEntryForm(
                initialDraft = LoanEntryDraft(
                    personName = "  Aarav  ",
                    direction = LoanDirection.LENT,
                    amountText = "1250.50",
                    currency = "INR",
                ),
                error = null,
                isSaving = false,
                onInputChanged = {},
                onSubmit = { name, direction, amount, currency, note ->
                    submittedName = name
                    submittedDirection = direction
                    submittedAmount = amount
                    submittedCurrency = currency
                    submittedNote = note
                },
            )
        }

        composeTestRule
            .onNodeWithText("Save entry", useUnmergedTree = true)
            .performClick()

        assertEquals("Aarav", submittedName)
        assertEquals(LoanDirection.LENT, submittedDirection)
        assertEquals(BigDecimal("1250.50"), submittedAmount)
        assertEquals("INR", submittedCurrency)
        assertNull(submittedNote)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.Padding.content),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content()
                }
            }
        }
    }
}
