package com.pennywiseai.tracker.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Home panel layout is persisted as a plain string and reconciled against the current
 * [HomePanel] entries on every read, instead of via a schema migration. These tests pin the
 * behaviour that makes that safe across app updates in both directions.
 */
class HomePanelLayoutTest {

    @Test
    fun `default layout covers every panel in declaration order`() {
        assertEquals(
            HomePanel.entries.toList(),
            HomePanelLayout.DEFAULT.map { it.panel }
        )
    }

    @Test
    fun `encode then decode round-trips order and enabled state`() {
        val layout = listOf(
            HomePanelState(HomePanel.ACCOUNTS, enabled = true),
            HomePanelState(HomePanel.MONTH_SUMMARY, enabled = false),
        ) + HomePanel.entries
            .filter { it != HomePanel.ACCOUNTS && it != HomePanel.MONTH_SUMMARY }
            .map { HomePanelState(it, it.defaultEnabled) }

        assertEquals(layout, HomePanelLayout.decode(HomePanelLayout.encode(layout)))
    }

    @Test
    fun `null or blank falls back to the default layout`() {
        assertEquals(HomePanelLayout.DEFAULT, HomePanelLayout.decode(null))
        assertEquals(HomePanelLayout.DEFAULT, HomePanelLayout.decode(""))
        assertEquals(HomePanelLayout.DEFAULT, HomePanelLayout.decode("   "))
    }

    @Test
    fun `unknown panel ids are dropped rather than crashing`() {
        val decoded = HomePanelLayout.decode("ACCOUNTS:1,PANEL_FROM_THE_FUTURE:1,MONTH_SUMMARY:0")

        assertTrue(decoded.none { it.panel.name == "PANEL_FROM_THE_FUTURE" })
        assertEquals(HomePanel.ACCOUNTS, decoded.first().panel)
        assertEquals(HomePanel.MONTH_SUMMARY, decoded[1].panel)
    }

    @Test
    fun `a panel added in a later version is appended with its default`() {
        // A layout stored before SUBSCRIPTIONS_DUE existed.
        val decoded = HomePanelLayout.decode("ACCOUNTS:1,MONTH_SUMMARY:1")

        assertEquals(HomePanel.entries.size, decoded.size)
        val appended = decoded.single { it.panel == HomePanel.SUBSCRIPTIONS_DUE }
        assertEquals(HomePanel.SUBSCRIPTIONS_DUE.defaultEnabled, appended.enabled)
        // The two stored panels keep their positions at the front.
        assertEquals(listOf(HomePanel.ACCOUNTS, HomePanel.MONTH_SUMMARY), decoded.take(2).map { it.panel })
    }

    @Test
    fun `malformed entries are skipped without losing the rest`() {
        val decoded = HomePanelLayout.decode("ACCOUNTS:1,,garbage,MONTH_SUMMARY:0:extra,ACTIVITY_HEATMAP:0")

        assertEquals(HomePanel.ACCOUNTS, decoded.first().panel)
        assertEquals(HomePanel.ACTIVITY_HEATMAP, decoded[1].panel)
        assertEquals(false, decoded[1].enabled)
        assertEquals(HomePanel.entries.size, decoded.size)
    }

    @Test
    fun `an unreadable enabled flag fails towards showing the panel`() {
        val decoded = HomePanelLayout.decode("ACCOUNTS:,MONTH_SUMMARY:yes")

        assertTrue(decoded.single { it.panel == HomePanel.ACCOUNTS }.enabled)
        assertTrue(decoded.single { it.panel == HomePanel.MONTH_SUMMARY }.enabled)
    }

    @Test
    fun `a duplicated id keeps only its first occurrence`() {
        val decoded = HomePanelLayout.decode("ACCOUNTS:0,ACCOUNTS:1")

        assertEquals(1, decoded.count { it.panel == HomePanel.ACCOUNTS })
        assertEquals(false, decoded.single { it.panel == HomePanel.ACCOUNTS }.enabled)
    }

    @Test
    fun `reorder moves a panel and preserves every other position`() {
        val moved = HomePanelLayout.reorder(HomePanelLayout.DEFAULT, from = 0, to = 2)

        assertEquals(HomePanelLayout.DEFAULT[0].panel, moved[2].panel)
        assertEquals(HomePanelLayout.DEFAULT[1].panel, moved[0].panel)
        assertEquals(HomePanelLayout.DEFAULT.size, moved.size)
        assertEquals(HomePanelLayout.DEFAULT.map { it.panel }.toSet(), moved.map { it.panel }.toSet())
    }

    @Test
    fun `reorder clamps out-of-range targets instead of throwing`() {
        // Dragging the first panel past the bottom lands it at the bottom.
        val moved = HomePanelLayout.reorder(HomePanelLayout.DEFAULT, from = 0, to = 99)
        assertEquals(HomePanelLayout.DEFAULT.first().panel, moved.last().panel)
        assertEquals(HomePanelLayout.DEFAULT.size, moved.size)

        // An out-of-range source is a no-op.
        assertEquals(
            HomePanelLayout.DEFAULT,
            HomePanelLayout.reorder(HomePanelLayout.DEFAULT, from = -1, to = 0)
        )
    }

    @Test
    fun `setEnabled changes only the named panel`() {
        val updated = HomePanelLayout.setEnabled(
            HomePanelLayout.DEFAULT,
            HomePanel.ACCOUNTS,
            enabled = false
        )

        assertEquals(false, updated.single { it.panel == HomePanel.ACCOUNTS }.enabled)
        assertEquals(
            HomePanelLayout.DEFAULT.filter { it.panel != HomePanel.ACCOUNTS },
            updated.filter { it.panel != HomePanel.ACCOUNTS }
        )
    }
}
