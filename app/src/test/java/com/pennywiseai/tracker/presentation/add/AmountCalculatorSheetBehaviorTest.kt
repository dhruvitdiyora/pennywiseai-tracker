package com.pennywiseai.tracker.presentation.add

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What "Use amount" may hand back: only a positive figure, rounded to the
 * currency's decimal places. The amount fields strip everything but digits and
 * the point, so an unguarded -5 would have been stored as 5.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AmountCalculatorSheetBehaviorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var applied: String? = null

    @Test
    fun negativeResultShowsAHintAndCannotBeApplied() {
        setContent(initialAmount = "10", currencyCode = "INR")

        press("Subtract", "Digit 1", "Digit 5")

        composeTestRule.onNodeWithText("Amount must be greater than 0").assertExists()
        composeTestRule.onNodeWithText("Use amount").assertIsNotEnabled()
        assertNull(applied)
    }

    @Test
    fun zeroResultShowsAHintAndCannotBeApplied() {
        setContent(initialAmount = "", currencyCode = "INR")

        press("Digit 0")

        composeTestRule.onNodeWithText("Amount must be greater than 0").assertExists()
        composeTestRule.onNodeWithText("Use amount").assertIsNotEnabled()
    }

    @Test
    fun resultThatRoundsToZeroCannotBeApplied() {
        // 1 / 1000 = 0.001, which is 0.00 at INR's two decimal places.
        setContent(initialAmount = "1", currencyCode = "INR")

        press("Divide", "Digit 1", "Digit 0", "Digit 0", "Digit 0")

        composeTestRule.onNodeWithText("Amount must be greater than 0").assertExists()
        composeTestRule.onNodeWithText("Use amount").assertIsNotEnabled()
    }

    @Test
    fun positiveResultHasNoHintAndIsApplied() {
        setContent(initialAmount = "12", currencyCode = "INR")

        press("Add", "Digit 3")

        composeTestRule.onNodeWithText("Amount must be greater than 0").assertDoesNotExist()
        composeTestRule.onNodeWithText("Use amount").assertIsEnabled().performClick()
        assertEquals("15", applied)
    }

    @Test
    fun divisionIsRoundedToTheCurrencyBeforeItIsApplied() {
        setContent(initialAmount = "100", currencyCode = "INR")

        press("Divide", "Digit 3")
        composeTestRule.onNodeWithText("33.33").assertExists()
        composeTestRule.onNodeWithText("Use amount").performClick()

        assertEquals("33.33", applied)
    }

    @Test
    fun aCurrencyWithoutMinorUnitsRoundsToWholeNumbers() {
        setContent(initialAmount = "100", currencyCode = "JPY")

        press("Divide", "Digit 3")
        composeTestRule.onNodeWithText("Use amount").performClick()

        assertEquals("33", applied)
    }

    @Test
    fun withoutACurrencyTheResultIsRoundedToTwoPlaces() {
        setContent(initialAmount = "100", currencyCode = null)

        press("Divide", "Digit 3")
        composeTestRule.onNodeWithText("Use amount").performClick()

        assertEquals("33.33", applied)
    }

    private fun press(vararg descriptions: String) {
        descriptions.forEach { composeTestRule.onNodeWithContentDescription(it).performClick() }
    }

    private fun setContent(initialAmount: String, currencyCode: String?) {
        applied = null
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = false,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AmountCalculatorSheet(
                        initialAmount = initialAmount,
                        onDismiss = {},
                        onApply = { applied = it },
                        currencyCode = currencyCode,
                    )
                }
            }
        }
    }
}

class CurrencyRoundingTest {
    @Test
    fun fractionDigitsFollowTheCurrency() {
        assertEquals(2, currencyFractionDigits("INR"))
        assertEquals(2, currencyFractionDigits("USD"))
        assertEquals(0, currencyFractionDigits("JPY"))
        assertEquals(3, currencyFractionDigits("KWD"))
    }

    @Test
    fun unknownBlankOrPseudoCurrenciesFallBackToTwoDigits() {
        assertEquals(2, currencyFractionDigits(null))
        assertEquals(2, currencyFractionDigits(""))
        assertEquals(2, currencyFractionDigits("not-a-code"))
        // "XXX" is a valid ISO pseudo-currency whose fraction digits are -1.
        assertEquals(2, currencyFractionDigits("XXX"))
    }

    @Test
    fun roundingIsHalfUpAtTheCurrencyPrecision() {
        assertEquals(BigDecimal("2.68"), roundToCurrency(BigDecimal("2.675"), "INR"))
        assertEquals(BigDecimal("34"), roundToCurrency(BigDecimal("33.5"), "JPY"))
        assertEquals(BigDecimal("0.125"), roundToCurrency(BigDecimal("0.1245"), "KWD"))
    }

    @Test
    fun longDivisionResultsAreCut() {
        val oneThird = BigDecimal("100").divide(BigDecimal("3"), java.math.MathContext.DECIMAL128)
        assertEquals(BigDecimal("33.33"), roundToCurrency(oneThird, "INR"))
    }
}
