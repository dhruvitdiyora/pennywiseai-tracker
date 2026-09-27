package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.presentation.loans.LendBorrowEntrySheet
import com.pennywiseai.tracker.presentation.loans.LoanListItem
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
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
fun PersonDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLoan: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var showAddEntry by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    PersonDetailScreenContent(
        state = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToLoan = onNavigateToLoan,
        onEdit = {
            viewModel.clearEditorError()
            showEditor = true
        },
        onAddEntry = {
            viewModel.clearEntryError()
            showAddEntry = true
        },
        onDeleteOrArchive = { showDeleteDialog = true },
        modifier = modifier,
    )

    val person = uiState.person
    if (showEditor && person != null) {
        PersonEditorSheet(
            person = person,
            state = uiState.editorState,
            onInputChanged = viewModel::clearEditorError,
            onDismiss = { showEditor = false },
            onSave = { name, phone, notes, category, color ->
                viewModel.savePerson(name, phone, notes, category, color) {
                    showEditor = false
                }
            },
        )
    }

    if (showAddEntry && person != null) {
        LendBorrowEntrySheet(
            initialCurrency = uiState.defaultEntryCurrency,
            initialPersonName = person.name,
            isPersonEditable = false,
            error = uiState.entryError,
            isSaving = uiState.isSavingEntry,
            onInputChanged = viewModel::clearEntryError,
            onDismiss = { if (!uiState.isSavingEntry) showAddEntry = false },
            onSubmit = { name, direction, amount, currency, note ->
                viewModel.addEntry(name, direction, amount, currency, note) {
                    showAddEntry = false
                }
            },
        )
    }

    if (showDeleteDialog && person != null) {
        val hasHistory = uiState.loans.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(stringResource(if (hasHistory) R.string.people_archive_title else R.string.people_delete_title))
            },
            text = {
                Text(stringResource(if (hasHistory) R.string.people_archive_description else R.string.people_delete_description))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteOrArchive(onNavigateBack) }) {
                    Text(stringResource(if (hasHistory) R.string.people_archive else R.string.people_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            },
        )
    }
}

@Composable
internal fun PersonDetailScreenContent(
    state: PersonDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateToLoan: (Long) -> Unit,
    onEdit: () -> Unit,
    onAddEntry: () -> Unit,
    onDeleteOrArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PennyWiseScaffold(
        modifier = modifier,
        title = state.person?.name ?: stringResource(R.string.people_person_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.people_back))
            }
        },
        actions = {
            if (state.person != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.people_edit_person))
                }
                IconButton(onClick = onDeleteOrArchive) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.people_delete_or_archive))
                }
            }
        },
        floatingActionButton = {
            if (state.person?.isArchived == false) {
                ExtendedFloatingActionButton(
                    onClick = onAddEntry,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.people_add_record)) },
                )
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.person == null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                PennyWiseEmptyState(
                    icon = Icons.Default.History,
                    headline = stringResource(R.string.people_not_found),
                    description = stringResource(R.string.people_not_found_description),
                )
            }

            else -> PersonLedger(
                person = state.person,
                loans = state.loans,
                state = state,
                paddingValues = paddingValues,
                onNavigateToLoan = onNavigateToLoan,
            )
        }
    }
}

@Composable
private fun PersonLedger(
    person: PersonEntity,
    loans: List<LoanEntity>,
    state: PersonDetailUiState,
    paddingValues: PaddingValues,
    onNavigateToLoan: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val categoryLabel = stringResource(R.string.people_category)
    val phoneLabel = stringResource(R.string.people_phone)
    val notesLabel = stringResource(R.string.people_notes)
    val details = listOfNotNull(
        person.category?.let { PersonDetailRow(Icons.Default.Label, categoryLabel, it) },
        person.phoneNumber?.let { PersonDetailRow(Icons.Default.Phone, phoneLabel, it) },
        person.notes?.let { PersonDetailRow(Icons.Default.Notes, notesLabel, it) },
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().overScrollVertical(),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = paddingValues.calculateTopPadding() + Spacing.md,
            bottom = paddingValues.calculateBottomPadding() + Dimensions.Component.fabScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
        flingBehavior = rememberOverscrollFlingBehavior { listState },
    ) {
        item { PersonHeroCard(person, state) }

        if (details.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(title = stringResource(R.string.people_details))
                    GroupedList {
                        details.forEachIndexed { index, detail ->
                            GroupedRow(
                                position = ListItemPosition.from(index, details.size),
                                minHeight = Dimensions.Component.listItemMinHeightTwoLine,
                            ) {
                                IconTile(
                                    icon = detail.icon,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                RowLabels(title = detail.label, subtitle = detail.value, subtitleMaxLines = 4)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                SectionHeaderV2(title = stringResource(R.string.people_balance))
                PersonBalanceRows(state)
            }
        }

        item {
            SectionHeaderV2(
                title = stringResource(R.string.people_records),
                subtitle = stringResource(R.string.people_record_count, loans.size),
            )
        }

        if (loans.isEmpty()) {
            item {
                PennyWiseEmptyState(
                    icon = Icons.Default.SwapHoriz,
                    headline = stringResource(R.string.people_no_records),
                    description = stringResource(R.string.people_no_records_description),
                )
            }
        } else {
            items(loans, key = { it.id }) { loan ->
                LoanListItem(loan = loan, onClick = { onNavigateToLoan(loan.id) })
            }
        }
    }
}

@Composable
private fun PersonHeroCard(person: PersonEntity, state: PersonDetailUiState) {
    val accent = parseProfileColor(person.color, MaterialTheme.colorScheme.primary)
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(Dimensions.Icon.emptyStateContainer),
                shape = CircleShape,
                color = accent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = person.initials(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColorFor(accent),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = when {
                        person.isArchived -> stringResource(R.string.people_archived)
                        state.summary?.activeLoanCount == 0 -> stringResource(R.string.people_settled_up)
                        else -> stringResource(R.string.people_active_records, state.summary?.activeLoanCount ?: 0)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PersonBalanceRows(state: PersonDetailUiState) {
    val summary = state.summary
    val currencies = ((summary?.lentByCurrency?.keys ?: emptySet()) +
        (summary?.borrowedByCurrency?.keys ?: emptySet())).sorted()
    if (currencies.isEmpty()) {
        PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.people_no_open_balance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    GroupedList {
        currencies.forEachIndexed { index, currency ->
            val lent = summary?.lentByCurrency?.get(currency) ?: java.math.BigDecimal.ZERO
            val borrowed = summary?.borrowedByCurrency?.get(currency) ?: java.math.BigDecimal.ZERO
            GroupedRow(
                position = ListItemPosition.from(index, currencies.size),
                minHeight = Dimensions.Component.listItemMinHeightTwoLine,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(currency, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BalanceFigure(
                            label = stringResource(R.string.home_loans_owed_to_you),
                            amount = CurrencyFormatter.formatCurrency(lent, currency),
                            color = MaterialTheme.colorScheme.expense,
                        )
                        BalanceFigure(
                            label = stringResource(R.string.home_loans_you_owe),
                            amount = CurrencyFormatter.formatCurrency(borrowed, currency),
                            color = MaterialTheme.colorScheme.income,
                            alignEnd = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceFigure(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = PennyWiseText.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount, style = PennyWiseText.amountMedium, color = color)
    }
}

private data class PersonDetailRow(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String,
)
