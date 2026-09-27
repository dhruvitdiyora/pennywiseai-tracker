package com.pennywiseai.tracker.presentation.transactions

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptShareIntentTest {
    @Test
    fun shareIntent_grantsReadAccessAndCarriesReceipt() {
        val receiptUri = Uri.parse("content://app.test/receipts/receipt.jpg")

        val intent = buildReceiptShareIntent(receiptUri, "Purchase")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/jpeg", intent.type)
        assertEquals(
            receiptUri,
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertEquals("Purchase", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
