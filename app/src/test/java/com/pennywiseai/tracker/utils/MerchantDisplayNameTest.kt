package com.pennywiseai.tracker.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantDisplayNameTest {

    @Test
    fun `real merchant names are kept`() {
        listOf(
            "Netflix",
            "Swiggy",
            "AMAZON PAY INDIA PRIVATE LIMITED",
            "Yahoo!",
            "Hotpot Kitchen",
            "Call of Duty Store",
            "merchant@upi",
        ).forEach { assertFalse(it, MerchantDisplayName.looksLikeSmsBody(it)) }
    }

    @Test
    fun `sms boilerplate is detected`() {
        listOf(
            "Not you? Call the bank",
            "If not done by you, report it",
            "Dear Customer your account",
            "Avl Bal is low",
            "Visit https://example.com for details",
            "your card ending was used at a store today for a purchase",
            "x".repeat(61),
        ).forEach { assertTrue(it, MerchantDisplayName.looksLikeSmsBody(it)) }
    }

    @Test
    fun `blank merchant is not treated as sms body`() {
        assertFalse(MerchantDisplayName.looksLikeSmsBody(null))
        assertFalse(MerchantDisplayName.looksLikeSmsBody("  "))
    }

    @Test
    fun `title falls back to bank then unknown label`() {
        assertEquals("Netflix", MerchantDisplayName.titleFor("Netflix", "Test Bank", "Unknown"))
        assertEquals("Test Bank", MerchantDisplayName.titleFor("Not you? Call us", "Test Bank", "Unknown"))
        assertEquals("Unknown", MerchantDisplayName.titleFor("Not you? Call us", null, "Unknown"))
        assertEquals("Unknown", MerchantDisplayName.titleFor("", " ", "Unknown"))
    }
}
