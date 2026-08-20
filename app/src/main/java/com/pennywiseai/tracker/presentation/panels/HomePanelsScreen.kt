package com.pennywiseai.tracker.presentation.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.preferences.HomePanelState
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Choose which panels Home shows, and in what order.
 *
 * Reached from More → Home panels. Every entry in
 * [com.pennywiseai.tracker.data.preferences.HomePanel] is something Home can actually render,
 * so no toggle here is a no-op.
 *
 * Reordering is up/down buttons rather than long-press drag. Drag is the nicer interaction
 * and is what this should become — but it is fiddly enough that it wants verifying on a real
 * device, and buttons are unambiguous, reachable one-handed, and work with TalkBack for free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePanelsScreen(
    viewModel: HomePanelsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val panels by viewModel.panels.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = "Home panels",
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actionContent = {
                    TextButton(onClick = viewModel::resetToDefault) { Text("Reset") }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = Spacing.Layout.scrollBottomPadding
            )
        ) {
            item {
                Text(
                    text = "Switch a panel off to hide it from Home, or move it to change " +
                        "where it appears.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Dimensions.Padding.content,
                        vertical = Spacing.sm
                    )
                )
            }

            item {
                GroupedList(
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                ) {
                    panels.forEachIndexed { index, state ->
                        PanelRow(
                            state = state,
                            position = ListItemPosition.from(index, panels.size),
                            canMoveUp = index > 0,
                            canMoveDown = index < panels.lastIndex,
                            onMoveUp = { viewModel.move(index, index - 1) },
                            onMoveDown = { viewModel.move(index, index + 1) },
                            onToggle = { viewModel.setEnabled(state.panel, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelRow(
    state: HomePanelState,
    position: ListItemPosition,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    GroupedRow(
        position = position,
        onClick = { onToggle(!state.enabled) }
    ) {
        RowLabels(
            title = state.panel.title,
            subtitle = state.panel.description,
            titleMaxLines = 2
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MoveButton(
                enabled = canMoveUp,
                onClick = onMoveUp,
                label = "Move ${state.panel.title} up",
                up = true
            )
            MoveButton(
                enabled = canMoveDown,
                onClick = onMoveDown,
                label = "Move ${state.panel.title} down",
                up = false
            )
        }
        Switch(
            checked = state.enabled,
            onCheckedChange = onToggle,
            // The row already announces the panel and toggles on tap; the switch repeating
            // it makes TalkBack read the same thing twice.
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
private fun MoveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String,
    up: Boolean,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(Dimensions.Component.iconButton)
            .semanticsLabel(label)
    ) {
        Icon(
            imageVector = if (up) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.inline)
        )
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(Modifier.clearAndSetSemantics { contentDescription = label })
