package com.pennywiseai.tracker.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No `Context` needed -- [IconCatalog.all] is a plain, Context-free list (see its
 * kdoc). Runs as a plain JVM unit test; there is no Robolectric in this project.
 */
class IconCatalogTest {

    @Test
    fun `catalog has at least 490 entries`() {
        // Guards against a partial asset copy -- see docs/ui-revamp/01-icon-assets.md.
        assertTrue(
            "expected >= 490 entries, got ${IconCatalog.all.size}",
            IconCatalog.all.size >= 490
        )
    }

    @Test
    fun `no duplicate iconName`() {
        val names = IconCatalog.all.map { it.iconName }
        val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("duplicate iconName values: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `no duplicate resourceId`() {
        val ids = IconCatalog.all.map { it.resourceId }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("duplicate resourceId values: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `every resourceId is non-zero`() {
        // R constants are plain ints, available on the JVM without Android.
        val zero = IconCatalog.all.filter { it.resourceId == 0 }
        assertTrue("entries with resourceId == 0: $zero", zero.isEmpty())
    }

    @Test
    fun `every iconName matches the type_ naming convention`() {
        val pattern = Regex("^type_[a-z0-9_]+$")
        val offenders = IconCatalog.all.filter { !pattern.matches(it.iconName) }
        assertTrue("iconName values not matching ^type_[a-z0-9_]+\$: $offenders", offenders.isEmpty())
    }

    @Test
    fun `every iconName has a matching file on disk`() {
        // The name-asset pairing is what the whole stable-resolution scheme rests
        // on (a name with no asset silently renders the fallback icon), and it is
        // the one thing a JVM test can still verify without a resource table.
        // Gradle runs unit tests with the module as the working directory.
        val dir = File("src/main/res/drawable-nodpi")
        assertTrue("expected drawable-nodpi dir to exist at ${dir.absolutePath}", dir.exists())

        val onDisk = dir.list()!!
            .filter { it.startsWith("type_") }
            .map { it.substringBeforeLast('.') }
            .toSet()

        val missing = IconCatalog.all.filter { it.iconName !in onDisk }
        assertTrue("iconName with no matching file on disk: ${missing.map { it.iconName }}", missing.isEmpty())
    }

    @Test
    fun `default is type_finance_money_bag`() {
        assertEquals("type_finance_money_bag", IconCatalog.default.iconName)
    }
}
