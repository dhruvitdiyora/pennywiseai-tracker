package com.pennywiseai.tracker.utils

import java.util.Locale

/**
 * Display-only guard for merchant names that are really fragments of the SMS
 * body (e.g. a parser that captured "Not you? Call ..." as the merchant).
 *
 * Nothing here touches stored data — screens use it to pick a sensible title
 * (the bank name, or a localized "Unknown merchant") instead of showing the
 * raw text.
 */
object MerchantDisplayName {

    /** Longer than any real merchant / payee name we render as a row title. */
    private const val MAX_PLAUSIBLE_LENGTH = 60

    /** A merchant name with this many words or more reads like a sentence. */
    private const val SENTENCE_WORD_COUNT = 8

    /** Phrases that only ever appear in the SMS boilerplate, never in a payee name. */
    private val SMS_BODY_MARKERS = listOf(
        "not you",
        "if not done",
        "if not you",
        "not done by you",
        "call us",
        "call on",
        "call at",
        "to block",
        "dear customer",
        "avl bal",
        "avl. bal",
        "available bal",
        "a/c ",
        "a/c no",
        "otp",
        "click here",
        "helpline",
        "customer care",
        "toll free",
        "has been debited",
        "has been credited",
        "is debited",
        "is credited",
        "http://",
        "https://",
        "www.",
    )

    /** True when [merchant] looks like SMS body text rather than a payee name. */
    fun looksLikeSmsBody(merchant: String?): Boolean {
        val text = merchant?.trim().orEmpty()
        if (text.isEmpty()) return false
        if (text.length > MAX_PLAUSIBLE_LENGTH) return true
        if ('?' in text) return true
        val lower = text.lowercase(Locale.ROOT)
        if (SMS_BODY_MARKERS.any { marker -> marker.isWholePhraseIn(lower) }) return true
        return text.split(' ', '\t', '\n').count { it.isNotBlank() } >= SENTENCE_WORD_COUNT
    }

    /**
     * The title to show for a merchant: [merchant] itself when it is plausible,
     * otherwise [bankName] when known, otherwise [unknownLabel].
     */
    fun titleFor(merchant: String?, bankName: String?, unknownLabel: String): String {
        val text = merchant?.trim().orEmpty()
        if (text.isNotEmpty() && !looksLikeSmsBody(text)) return text
        return bankName?.trim()?.takeIf { it.isNotEmpty() } ?: unknownLabel
    }

    /** Matches [this] in [haystack] only at word boundaries ("otp" not in "hotpot"). */
    private fun String.isWholePhraseIn(haystack: String): Boolean {
        var from = 0
        while (true) {
            val at = haystack.indexOf(this, from)
            if (at < 0) return false
            val beforeOk = at == 0 || !haystack[at - 1].isLetterOrDigit()
            val endIndex = at + length
            val afterOk = endIndex >= haystack.length ||
                !this.last().isLetterOrDigit() ||
                !haystack[endIndex].isLetterOrDigit()
            if (beforeOk && afterOk) return true
            from = at + 1
        }
    }
}
