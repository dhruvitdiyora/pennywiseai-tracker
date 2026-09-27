package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter

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
    PennyWiseScaffold(
        modifier = modifier,
        title = stringResource(R.string.people_contacts_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.people_back),
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPerson,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.people_add_person)) },
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            state.people.isEmpty() && state.query.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().overScrollVertical(),
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
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onQueryChanged,
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                            singleLine = true,
                            label = { Text(stringResource(R.string.people_search)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = if (state.query.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onQueryChanged("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.people_clear_search),
                                        )
                                    }
                                }
                            } else null,
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

@Composable
private fun PersonCard(
    row: PersonWithSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val person = row.person
    val fallback = MaterialTheme.colorScheme.primary
    val color = parseProfileColor(person.color, fallback)
    PennyWiseCardV2(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine * 2),
        onClick = onClick,
        contentPadding = Dimensions.Padding.cardCompact,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(Dimensions.Icon.avatarLarge),
                shape = CircleShape,
                color = color.copy(alpha = Dimensions.Alpha.tonalIconContainer),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = person.initials(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.people_edit_named, person.name),
                    modifier = Modifier.size(Dimensions.Icon.inline),
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = person.category ?: stringResource(R.string.people_contact),
                style = PennyWiseText.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            PersonBalanceLabel(row)
        }
    }
}

@Composable
private fun PersonBalanceLabel(row: PersonWithSummary) {
    val entries = row.summary.netByCurrency.entries.sortedBy { it.key }
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.people_settled_up),
            style = PennyWiseText.amountSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    entries.take(2).forEach { (currency, amount) ->
        val positive = amount.signum() > 0
        Text(
            text = stringResource(
                if (positive) R.string.people_owed_to_you_amount else R.string.people_you_owe_amount,
                CurrencyFormatter.formatCurrency(amount.abs(), currency),
            ),
            style = PennyWiseText.amountSmall,
            color = if (positive) MaterialTheme.colorScheme.expense else MaterialTheme.colorScheme.income,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun PersonEntity.initials(): String {
    val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}
