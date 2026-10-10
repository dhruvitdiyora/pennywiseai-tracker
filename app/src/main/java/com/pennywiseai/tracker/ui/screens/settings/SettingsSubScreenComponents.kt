package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/*
 * Shared chrome for the Settings sub-screens (Appearance, About, Licenses,
 * Data & privacy, FAQ), ported from Cashiro's settings pages: a large title
 * that collapses into the compact bar, a tonal back button, and grouped rows
 * with tonal icon tiles. Settings itself already uses the same GroupedList /
 * GroupedRow / IconTile primitives, so these screens read as one family with
 * it instead of each re-assembling its own scaffold.
 */

/** The tonal back button for a Settings sub-screen (mirrors in RTL). */
@Composable
internal fun SettingsBackButton(
    onClick: () -> Unit,
    contentDescription: String,
) {
    TonalNavigationButton(
        onClick = onClick,
        contentDescription = contentDescription,
    )
}

/**
 * Scaffold for a scrolling Settings sub-screen.
 *
 * The title starts large and collapses into the compact bar as the content
 * scrolls; the content runs under the blurred bar (it adds the bar's height
 * as its own top inset rather than being clipped by the scaffold).
 *
 * @param horizontalPadding screen gutter around [content]. Pass
 * [Spacing.none] when the content manages its own gutter so a horizontally
 * scrolling row can bleed to the screen edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSubScreen(
    title: String,
    backContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Dimensions.Padding.content,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    PennyWiseScaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        customTopBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = title,
                hasBackButton = true,
                navigationContent = {
                    SettingsBackButton(
                        onClick = onNavigateBack,
                        contentDescription = backContentDescription,
                    )
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = Dimensions.Padding.content,
                    bottom = Spacing.Layout.scrollBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
            content = content,
        )
    }
}

/**
 * A section heading with the content it labels. The heading sits closer to
 * its content ([Spacing.Layout.headerToContent]) than to the section above, so
 * place sections in a column spaced by [Spacing.Layout.sectionGap].
 */
@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    headerHorizontalPadding: Dp = Spacing.none,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
    ) {
        SectionHeaderV2(
            title = title,
            modifier = Modifier.padding(horizontal = headerHorizontalPadding),
            leading = leading,
            topSpacing = Spacing.none,
        )
        content()
    }
}

/**
 * One row of a grouped Settings list: a tonal icon tile, title with optional
 * supporting text, and an optional trailing affordance (chevron for in-app
 * navigation, export arrow for an external link). Rows without [onClick] are
 * informational.
 */
@Composable
internal fun SettingsIconRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    title: String,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    subtitleMaxLines: Int = 2,
    minHeight: Dp = Dimensions.Component.minTouchTarget,
) {
    GlassGroupedRow(
        position = position,
        modifier = modifier,
        onClick = onClick,
        minHeight = minHeight,
    ) {
        IconTile(
            icon = icon,
            containerColor = iconContainerColor,
            contentColor = iconContentColor,
        )
        RowLabels(
            title = title,
            subtitle = subtitle,
            subtitleMaxLines = subtitleMaxLines,
        )
        if (trailingIcon != null) {
            // A hint, not a control: 20dp keeps it from competing with the
            // leading tile, matching the chevrons on the Settings screen.
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.Icon.inline),
            )
        }
    }
}
