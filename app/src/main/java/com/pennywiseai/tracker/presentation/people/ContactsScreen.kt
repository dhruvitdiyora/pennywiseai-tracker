package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeopleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    var editedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    ContactsScreenContent(
        state = uiState,
        onQueryChanged = viewModel::updateQuery,
        onNavigateBack = onNavigateBack,
        onAddPerson = {
            viewModel.clearEditorError()
            editedPerson = null
            showEditor = true
        },
        onEditPerson = { person ->
            viewModel.clearEditorError()
            editedPerson = person
            showEditor = true
        },
        onPersonClick = { onNavigateToPerson(it.person.id) },
        modifier = modifier,
    )

    if (showEditor) {
        PersonEditorSheet(
            person = editedPerson,
            state = editorState,
            onInputChanged = viewModel::clearEditorError,
            onDismiss = {
                viewModel.clearEditorError()
                showEditor = false
            },
            onSave = { name, phone, notes, category, color ->
                viewModel.savePerson(
                    person = editedPerson,
                    name = name,
                    phoneNumber = phone,
                    notes = notes,
                    category = category,
                    color = color,
                    onSaved = { showEditor = false },
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactsScreenContent(
    state: PeopleUiState,
    onQueryChanged: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onAddPerson: () -> Unit,
    onEditPerson: (PersonEntity) -> Unit,
    onPersonClick: (PersonWithSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val gridState = rememberLazyGridState()
    // The add button carries its label only while the grid is at the top.
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.people_contacts_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.people_back),
                    )
                },
                hazeState = hazeState,
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.people_add_person),
                onClick = onAddPerson,
                expanded = fabExpanded,
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            state.people.isEmpty() && state.query.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    PennyWiseEmptyState(
                        icon = Icons.Default.People,
                        headline = stringResource(R.string.people_empty_title),
                        description = stringResource(R.string.people_empty_description),
                        actionLabel = stringResource(R.string.people_add_person),
                        onAction = onAddPerson,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                        .background(MaterialTheme.colorScheme.background)
                        .overScrollVertical(),
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = paddingValues.calculateTopPadding() + Spacing.md,
                        bottom = paddingValues.calculateBottomPadding() + Dimensions.Component.fabScrollClearance,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    flingBehavior = rememberOverscrollFlingBehavior { gridState },
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PeopleSearchField(
                            query = state.query,
                            onQueryChange = onQueryChanged,
                            placeholder = stringResource(R.string.people_search),
                            clearDescription = stringResource(R.string.people_clear_search),
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }

                    if (state.people.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PennyWiseEmptyState(
                                icon = Icons.Default.Search,
                                headline = stringResource(R.string.people_no_results),
                                description = stringResource(R.string.people_no_results_description),
                            )
                        }
                    } else {
                        items(state.people, key = { it.person.id }) { row ->
                            PersonCard(
                                row = row,
                                onClick = { onPersonClick(row) },
                                onEdit = { onEditPerson(row.person) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** How strongly the decorative initials show through a contact tile. */
private const val TILE_WATERMARK_ALPHA = 0.25f

/** How many currency balances a tile spells out before summarising the rest. */
private const val TILE_MAX_BALANCES = 2

/**
 * One contact as a tile in the contact's own colour: the initials as a large
 * watermark, the name and relationship at the bottom, and the open balance as
 * pills (one per currency, never added together) that stay legible on any
 * colour. The pencil edits the contact; the rest of the tile opens it.
 */
@Composable
private fun PersonCard(
    row: PersonWithSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val person = row.person
    val color = parseProfileColor(person.color, MaterialTheme.colorScheme.primary)
    val onColor = contentColorOn(color)

    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = color,
        // The fill is the container; an outline would only add a seam.
        border = BorderStroke(Dimensions.Component.hairline, Color.Transparent),
        onClick = onClick,
        contentPadding = Dimensions.Padding.none,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine * TILE_HEIGHT_MULTIPLIER)
                .padding(Dimensions.Padding.cardCompact),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = person.initials(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = Spacing.sm),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = onColor.copy(alpha = TILE_WATERMARK_ALPHA),
                    maxLines = 1,
                )
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = onColor),
                ) {
                    Icon(
                        imageVector = Iconax.Edit2,
                        contentDescription = stringResource(R.string.people_edit_named, person.name),
                        modifier = Modifier.size(Dimensions.Icon.inline),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = person.category ?: stringResource(R.string.people_contact),
                    style = MaterialTheme.typography.labelMedium,
                    color = onColor.copy(alpha = Dimensions.Alpha.subtitle),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    modifier = Modifier.padding(top = Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    PersonBalancePills(row)
                }
            }
        }
    }
}

/** Tile height as a multiple of a two-line row (about the 190dp Cashiro uses). */
private const val TILE_HEIGHT_MULTIPLIER = 2.5f

@Composable
private fun PersonBalancePills(row: PersonWithSummary) {
    val entries = row.summary.netByCurrency.entries.sortedBy { it.key }
    if (entries.isEmpty()) {
        BalancePill(
            text = stringResource(R.string.people_settled_up),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    entries.take(TILE_MAX_BALANCES).forEach { (currency, amount) ->
        val positive = amount.signum() > 0
        BalancePill(
            text = stringResource(
                if (positive) R.string.people_owed_to_you_amount else R.string.people_you_owe_amount,
                CurrencyFormatter.formatCurrency(amount.abs(), currency),
            ),
            // Owed to you is the income colour, what you owe the expense colour.
            color = if (positive) MaterialTheme.colorScheme.income else MaterialTheme.colorScheme.expense,
        )
    }
    // A third open currency is never silently dropped.
    val hidden = entries.size - TILE_MAX_BALANCES
    if (hidden > 0) {
        BalancePill(
            text = stringResource(R.string.people_more_balances, hidden),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A balance on a coloured tile: a near-opaque surface pill, so it reads on any contact colour. */
@Composable
private fun BalancePill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = Dimensions.Alpha.high),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            style = PennyWiseText.amountSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
