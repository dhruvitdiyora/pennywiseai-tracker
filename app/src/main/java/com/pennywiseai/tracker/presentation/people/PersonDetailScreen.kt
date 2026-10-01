package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.presentation.loans.LendBorrowEntrySheet
import com.pennywiseai.tracker.presentation.loans.LoanListItem
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
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
import java.math.BigDecimal

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
            shape = MaterialTheme.shapes.extraLarge,
            iconContentColor = MaterialTheme.colorScheme.error,
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = state.person?.name ?: stringResource(R.string.people_person_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.people_back),
                    )
                },
                actionContent = {
                    if (state.person != null) {
                        PeopleTonalActionButton(
                            onClick = onEdit,
                            icon = Iconax.Edit2,
                            contentDescription = stringResource(R.string.people_edit_person),
                            endPadding = Spacing.sm,
                        )
                        PeopleTonalActionButton(
                            onClick = onDeleteOrArchive,
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.people_delete_or_archive),
                        )
                    }
                },
                hazeState = hazeState,
            )
        },
        floatingActionButton = {
            if (state.person?.isArchived == false) {
                PeopleExtendedFab(
                    label = stringResource(R.string.people_add_record),
                    onClick = onAddEntry,
                    expanded = fabExpanded,
                )
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.person == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
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
                listState = listState,
                hazeState = hazeState,
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
    listState: LazyListState,
    hazeState: HazeState,
    paddingValues: PaddingValues,
    onNavigateToLoan: (Long) -> Unit,
) {
    LazyColumn(
        state = listState,
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
        // Records sit 2dp apart as one connected list; every other block carries
        // its own spacing.
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
        flingBehavior = rememberOverscrollFlingBehavior { listState },
    ) {
        item(key = "header") {
            PersonHeader(
                person = person,
                state = state,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }

        item(key = "balance") {
            Column(
                modifier = Modifier.padding(bottom = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
            ) {
                SectionHeaderV2(title = stringResource(R.string.people_balance))
                PersonBalanceRows(state)
            }
        }

        item(key = "records-header") {
            SectionHeaderV2(
                title = stringResource(R.string.people_records),
                subtitle = stringResource(R.string.people_record_count, loans.size),
                modifier = Modifier.padding(bottom = Spacing.Layout.headerToContent),
            )
        }

        if (loans.isEmpty()) {
            item(key = "records-empty") {
                PennyWiseEmptyState(
                    icon = Icons.Default.SwapHoriz,
                    headline = stringResource(R.string.people_no_records),
                    description = stringResource(R.string.people_no_records_description),
                )
            }
        } else {
            itemsIndexed(loans, key = { _, loan -> loan.id }) { index, loan ->
                LoanListItem(
                    loan = loan,
                    onClick = { onNavigateToLoan(loan.id) },
                    shape = ListItemPosition.from(index, loans.size).toShape(),
                )
            }
        }
    }
}

/**
 * The person at the top of their ledger: a large avatar in their own colour,
 * where they stand (settled, archived, or how many records are open), their
 * relationship, the net they owe or are owed per currency, and their contact
 * details. The name is the screen title above.
 */
@Composable
private fun PersonHeader(
    person: PersonEntity,
    state: PersonDetailUiState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = parseProfileColor(person.color, scheme.primary)
    val nets = state.summary?.netByCurrency
        ?.entries
        ?.filter { it.value.signum() != 0 }
        ?.sortedBy { it.key }
        .orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PersonAvatar(
            initials = person.initials(),
            color = accent,
            size = PersonHeaderAvatarSize,
            textStyle = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = when {
                person.isArchived -> stringResource(R.string.people_archived)
                state.summary?.activeLoanCount == 0 -> stringResource(R.string.people_settled_up)
                else -> stringResource(R.string.people_active_records, state.summary?.activeLoanCount ?: 0)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        person.category?.let { category ->
            SubtitleTag(text = category, color = accent)
        }
        nets.forEach { (currency, amount) ->
            NetBalancePill(currency = currency, amount = amount)
        }
        person.phoneNumber?.let { phone ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = stringResource(R.string.people_phone),
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = scheme.onSurfaceVariant,
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        person.notes?.let { notes ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = scheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = stringResource(R.string.people_notes),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                }
            }
        }
    }
}

/** What this person nets to in one currency: owed to you is the income colour, what you owe the expense colour. */
@Composable
private fun NetBalancePill(currency: String, amount: BigDecimal) {
    val positive = amount.signum() > 0
    val color = if (positive) MaterialTheme.colorScheme.income else MaterialTheme.colorScheme.expense
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = Dimensions.Alpha.tonalIconContainer),
    ) {
        Text(
            text = stringResource(
                if (positive) R.string.people_owed_to_you_amount else R.string.people_you_owe_amount,
                CurrencyFormatter.formatCurrency(amount.abs(), currency),
            ),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = PennyWiseText.amountRow,
            color = color,
        )
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
            val lent = summary?.lentByCurrency?.get(currency) ?: BigDecimal.ZERO
            val borrowed = summary?.borrowedByCurrency?.get(currency) ?: BigDecimal.ZERO
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
                            label = stringResource(R.string.home_loans_label_owed_to_you),
                            amount = CurrencyFormatter.formatCurrency(lent, currency),
                            color = MaterialTheme.colorScheme.income,
                        )
                        BalanceFigure(
                            label = stringResource(R.string.home_loans_label_you_owe),
                            amount = CurrencyFormatter.formatCurrency(borrowed, currency),
                            color = MaterialTheme.colorScheme.expense,
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
    color: Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = PennyWiseText.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount, style = PennyWiseText.amountMedium, color = color)
    }
}
