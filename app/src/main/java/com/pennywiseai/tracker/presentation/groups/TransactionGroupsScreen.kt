package com.pennywiseai.tracker.presentation.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.repository.GroupSummary
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.theme.investment
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.sumByCurrency
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionGroupsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToGroupDetail: (Long) -> Unit = {},
    viewModel: TransactionGroupsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.groups_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.group_back)
                    )
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.groups_create),
                onClick = { viewModel.showCreateDialog() },
                expanded = fabExpanded
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    PennyWiseEmptyState(
                        icon = Icons.Default.Folder,
                        headline = stringResource(R.string.groups_empty_title),
                        description = stringResource(R.string.groups_empty_hint)
                    )
                }
            }

            else -> {
                val groups = uiState.groups
                // Totals across all groups, kept per currency: a rupee group and a
                // dollar group are never added into one figure.
                val expenseTotals = remember(groups) {
                    groups.flatMap { it.expenseByCurrency.values }
                        .sumByCurrency({ it.currency }, { it.amount })
                }
                val investedTotals = remember(groups) {
                    groups.flatMap { it.investedByCurrency.values }
                        .sumByCurrency({ it.currency }, { it.amount })
                }
                val incomeTotals = remember(groups) {
                    groups.flatMap { it.incomeByCurrency.values }
                        .sumByCurrency({ it.currency }, { it.amount })
                }
                val transactionTotal = remember(groups) { groups.sumOf { it.transactionCount } }
                val summarySubtitle = stringResource(
                    R.string.group_card_subtitle,
                    pluralStringResource(R.plurals.groups_group_count, groups.size, groups.size),
                    pluralStringResource(R.plurals.groups_transaction_count, transactionTotal, transactionTotal)
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                        .background(MaterialTheme.colorScheme.background)
                        .overScrollVertical(),
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() +
                            Dimensions.Component.fabScrollClearance
                    ),
                    // Group blocks sit 2dp apart as one connected list; the summary
                    // carries its own spacing.
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
                ) {
                    item(key = "summary") {
                        GroupSummaryCard(
                            title = stringResource(R.string.groups_summary_title),
                            subtitle = summarySubtitle,
                            expenseByCurrency = expenseTotals,
                            investedByCurrency = investedTotals,
                            incomeByCurrency = incomeTotals,
                            modifier = Modifier.padding(bottom = Spacing.md)
                        )
                    }

                    itemsIndexed(groups, key = { _, summary -> summary.group.id }) { index, summary ->
                        GroupRow(
                            summary = summary,
                            position = ListItemPosition.from(index, groups.size),
                            onClick = { onNavigateToGroupDetail(summary.group.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        GroupEditorSheet(
            title = stringResource(R.string.groups_new_title),
            confirmLabel = stringResource(R.string.groups_create_confirm),
            initialName = "",
            initialNote = null,
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, note -> viewModel.createGroup(name, note) }
        )
    }
}

/**
 * One group: a letter avatar, its name, note and transaction count, and on the
 * right what was spent, invested and received (each per currency, so mixed
 * currencies list side by side instead of being added).
 */
@Composable
private fun GroupRow(
    summary: GroupSummary,
    position: ListItemPosition,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    GroupedRow(
        position = position,
        onClick = onClick,
        contentPadding = PaddingValues(Dimensions.Padding.cardCompact),
        minHeight = Dimensions.Component.listItemMinHeightTwoLine,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
    ) {
        PersonAvatar(
            initials = summary.group.name.take(1).uppercase().ifEmpty { "?" },
            color = scheme.primary,
            size = Dimensions.Icon.avatarLarge,
            tinted = true
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(
                text = summary.group.name,
                style = PennyWiseText.rowTitle,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            summary.group.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = PennyWiseText.metadata,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.groups_transaction_count,
                    summary.transactionCount,
                    summary.transactionCount
                ),
                style = PennyWiseText.metadata,
                color = scheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            if (summary.hasExpense) {
                Text(
                    text = CurrencyFormatter.formatByCurrency(summary.expenseByCurrency, signPrefix = "-"),
                    style = PennyWiseText.amountRow,
                    color = scheme.expense
                )
            }
            if (summary.hasInvested) {
                Text(
                    text = stringResource(
                        R.string.group_card_invested,
                        CurrencyFormatter.formatByCurrency(summary.investedByCurrency)
                    ),
                    style = PennyWiseText.amountSmall,
                    color = scheme.investment
                )
            }
            if (summary.hasIncome) {
                Text(
                    text = CurrencyFormatter.formatByCurrency(summary.incomeByCurrency, signPrefix = "+"),
                    style = PennyWiseText.amountSmall,
                    color = scheme.income
                )
            }
        }
    }
}
