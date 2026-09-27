package com.pennywiseai.tracker.presentation.loans

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoanEntryLogicTest {
    @Test
    fun requiredFieldsRejectInvalidEntries() {
        assertEquals(
            LoanEntryError.PERSON_REQUIRED,
            validateLoanEntry(" ", BigDecimal.ONE, "INR"),
        )
        assertEquals(
            LoanEntryError.AMOUNT_REQUIRED,
            validateLoanEntry("Aarav", BigDecimal.ZERO, "INR"),
        )
        assertEquals(
            LoanEntryError.AMOUNT_REQUIRED,
            validateLoanEntry("Aarav", null, "INR"),
        )
        assertEquals(
            LoanEntryError.CURRENCY_REQUIRED,
            validateLoanEntry("Aarav", BigDecimal.ONE, " "),
        )
    }

    @Test
    fun validCurrencyTaggedEntryPasses() {
        assertNull(validateLoanEntry("Aarav", BigDecimal("1250.50"), "INR"))
    }
}
