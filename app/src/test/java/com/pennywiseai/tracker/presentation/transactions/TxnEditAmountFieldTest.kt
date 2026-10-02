package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The edit form's amount hero keeps what the user types. It used to render the
 * entity's amount back, so "12." came back as "12" (the dot vanished and the next
 * digit made it 120) and the field could not be emptied.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class TxnEditAmountFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val pushed = mutableListOf<String>()

    @Test
    fun typingADecimalOneDigitAtATimeKeepsTheDot() {
        setContent(initial = BigDecimal("12"))
        amountField().assert(hasText("12", substring = true))

        amountField().performTextReplacement("12.")
        amountField().assert(hasText("12.", substring = true))

        amountField().performTextReplacement("12.0")
        amountField().assert(hasText("12.0", substring = true))

        amountField().performTextReplacement("12.05")
        amountField().assert(hasText("12.05", substring = true))

        composeTestRule.runOnIdle {
            assertEquals(listOf("12.", "12.0", "12.05"), pushed)
        }
    }

    @Test
    fun theFieldCanBeEmptiedAndFlagsTheMissingAmount() {
        setContent(initial = BigDecimal("12"))
        composeTestRule.onNodeWithText("Amount is required").assertDoesNotExist()

        amountField().performTextReplacement("")
        composeTestRule.onNodeWithText("Amount is required").assertExists()
        composeTestRule.runOnIdle { assertEquals(listOf(""), pushed) }

        amountField().performTextReplacement("7")
        composeTestRule.onNodeWithText("Amount is required").assertDoesNotExist()
        amountField().assert(hasText("7", substring = true))
    }

    @Test
    fun zeroIsFlaggedAsNotPositive() {
        setContent(initial = BigDecimal("12"))

        amountField().performTextReplacement("0")
        composeTestRule.onNodeWithText("Amount must be a positive number").assertExists()

        amountField().performTextReplacement("0.5")
        composeTestRule.onNodeWithText("Amount must be a positive number").assertDoesNotExist()
    }

    @Test
    fun nonNumericCharactersAndASecondPointAreRejected() {
        setContent(initial = BigDecimal("12"))

        amountField().performTextReplacement("1a2")
        composeTestRule.runOnIdle { assertEquals("12", pushed.last()) }

        amountField().performTextReplacement("12.5")
        amountField().performTextReplacement("12.5.1")
        // The second point is refused, so the field stays at what it was.
        composeTestRule.runOnIdle { assertEquals("12.5", pushed.last()) }
        amountField().assert(hasText("12.5", substring = true))
    }

    private fun amountField() = composeTestRule.onNodeWithContentDescription("Amount")

    private fun setContent(initial: BigDecimal) {
        pushed.clear()
        composeTestRule.setContent {
            // Mirrors the view model: positive text becomes the entity amount, anything
            // else zero. The field must not depend on it after it has been seeded.
            var entityAmount by remember { mutableStateOf(initial) }
            PennyWiseTheme(
                darkTheme = false,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TxnEditAmountField(
                        transactionId = 1L,
                        initialAmount = entityAmount,
                        currency = "USD",
                        onAmountTextChange = { text ->
                            pushed += text
                            entityAmount = text.toBigDecimalOrNull()
                                ?.takeIf { it.signum() > 0 }
                                ?: BigDecimal.ZERO
                        },
                        onCurrencyChange = {},
                    )
                }
            }
        }
    }
}

class TxnEditAmountInputTest {
    @Test
    fun seedTextIsPlainDigitsWithoutTrailingZeros() {
        assertEquals("12.5", amountFieldText(BigDecimal("12.500")))
        assertEquals("1000", amountFieldText(BigDecimal("1E+3")))
        assertEquals("0", amountFieldText(BigDecimal("0.00")))
    }

    @Test
    fun sanitizeKeepsDigitsAndASingleDecimalPoint() {
        assertEquals("12.05", sanitizeAmountInput("12.05", previous = "12"))
        assertEquals("123", sanitizeAmountInput("1,2 3", previous = "12"))
        assertEquals("12.5", sanitizeAmountInput("12.5.1", previous = "12.5"))
        assertEquals("", sanitizeAmountInput("", previous = "12"))
    }

    @Test
    fun errorIsOnlyRaisedForEmptyOrNonPositiveText() {
        assertNull(amountFieldError("12.05"))
        assertNull(amountFieldError("12."))
        assertNull(amountFieldError(".5"))
        assertEquals(R.string.add_error_amount_required, amountFieldError(""))
        assertEquals(R.string.add_error_amount_required, amountFieldError("  "))
        assertEquals(R.string.txn_detail_error_amount_positive_number, amountFieldError("0"))
        assertEquals(R.string.txn_detail_error_amount_positive_number, amountFieldError("0.00"))
        assertEquals(R.string.txn_detail_error_amount_positive_number, amountFieldError("."))
    }
}
