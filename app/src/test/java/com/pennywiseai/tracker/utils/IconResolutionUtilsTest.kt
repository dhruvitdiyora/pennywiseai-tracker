package com.pennywiseai.tracker.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `Resources.getIdentifier` cannot be exercised on the JVM -- there is no
 * Robolectric in this project, and `app/build.gradle.kts` sets
 * `testOptions.unitTests.isReturnDefaultValues = true` deliberately instead, so
 * `getIdentifier` always returns the default `0`. This test covers only the pure
 * branches: empty/null input and the memoizing cache, using the same
 * `ContextWrapper(null)` fake pattern as
 * `receiver/NotificationActionReceiverTest.kt`.
 */
class IconResolutionUtilsTest {

    private val fakeContext = object : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): android.content.Context = this
    }

    @Test
    fun `nameToResId returns 0 for empty name`() {
        assertEquals(0, IconResolutionUtils.nameToResId(fakeContext, ""))
    }

    @Test
    fun `nameToResId returns 0 for null name`() {
        assertEquals(0, IconResolutionUtils.nameToResId(fakeContext, null))
    }

    @Test
    fun `nameToResId returns 0 for an unresolvable name`() {
        assertEquals(0, IconResolutionUtils.nameToResId(fakeContext, "definitely_not_an_icon"))
    }

    @Test
    fun `memo cache returns the same value twice`() {
        val first = IconResolutionUtils.nameToResId(fakeContext, "type_finance_money_bag")
        val second = IconResolutionUtils.nameToResId(fakeContext, "type_finance_money_bag")
        assertEquals(first, second)
    }

    @Test
    fun `isValidIconName is false for a blank name`() {
        assertEquals(false, IconResolutionUtils.isValidIconName(fakeContext, ""))
    }
}
