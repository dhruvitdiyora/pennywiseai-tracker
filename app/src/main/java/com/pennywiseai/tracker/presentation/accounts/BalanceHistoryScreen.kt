package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Bag
import com.pennywiseai.tracker.ui.icons.iconax.Card
import com.pennywiseai.tracker.ui.icons.iconax.Clock
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.History
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.Transfer
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.format.DateTimeFormatter

/** How much of an SMS a collapsed history record previews. */
private const val SMS_PREVIEW_CHARS = 80

/** A balance field accepts digits and one decimal point. */
private val BalanceInputPattern = Regex("^\\d*\\.?\\d*$")

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun BalanceHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: BalanceHistoryViewModel = hiltViewModel()
) {
    val balanceHistory by viewModel.history.collectAsStateWithLifecycle()
    val bankName = viewModel.bankName
    val accountLast4 = viewModel.accountLast4
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf<Long?>(null) }
    var expandedSources by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val clipboard = LocalClipboardManager.current

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // Records sit 2dp apart as one connected list; the summary carries the larger gap.
    val sectionGap = Spacing.md - Spacing.Layout.groupedListGap

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.balance_history_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                hazeState = hazeState
            )
        }
    ) { innerPadding ->
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
                top = Dimensions.Padding.content + innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Which account this is, and how many records it has.
            item(key = "summary") {
                PennyWiseCardV2(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = sectionGap),
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = Dimensions.Padding.card
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandIcon(
                            merchantName = bankName,
                            size = Dimensions.Icon.avatarLarge,
                            showBackground = true
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                        ) {
                            Text(
                                text = AccountBalanceEntity.accountLabel(bankName, accountLast4),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.balance_history_record_count,
                                    balanceHistory.size,
                                    balanceHistory.size
                                ),
                                style = PennyWiseText.rowSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (balanceHistory.isEmpty()) {
                item(key = "empty") {
                    PennyWiseEmptyState(
                        icon = Iconax.History,
                        headline = stringResource(R.string.balance_history_empty_title),
                        description = stringResource(R.string.balance_history_empty_description)
                    )
                }
            } else {
                // Balance History List
                itemsIndexed(balanceHistory, key = { _, balance -> balance.id }) { index, balance ->
                    val isLatest = index == 0
                    val isOnlyRecord = balanceHistory.size == 1
                    val isExpanded = expandedSources.contains(balance.id)
                    // Resolve the row's currency the same way the rest of the
                    // account UI does: a MANUAL account keeps its stored currency
                    // (e.g. MXN); SMS-tracked rows fall back to the bank base. (#631)
                    val rowCurrency = CurrencyFormatter.resolveAccountCurrency(
                        sourceType = balance.sourceType,
                        storedCurrency = balance.currency,
                        bankName = bankName
                    )

                    BalanceHistoryItem(
                        balance = balance,
                        position = ListItemPosition.from(index, balanceHistory.size),
                        isLatest = isLatest,
                        isOnlyRecord = isOnlyRecord,
                        isExpanded = isExpanded,
                        editingId = editingId,
                        editingValue = editingValue,
                        accountPrimaryCurrency = rowCurrency,
                        onEditClick = {
                            editingId = balance.id
                            editingValue = balance.balance.toPlainString()
                        },
                        onDeleteClick = {
                            showDeleteConfirmation = balance.id
                        },
                        onEditValueChange = { value ->
                            if (value.matches(BalanceInputPattern)) {
                                editingValue = value
                            }
                        },
                        onSaveEdit = {
                            editingValue.toBigDecimalOrNull()?.let { newBalance ->
                                viewModel.updateBalance(balance.id, newBalance)
                                editingId = null
                                editingValue = ""
                            }
                        },
                        onCancelEdit = {
                            editingId = null
                            editingValue = ""
                        },
                        onToggleExpand = {
                            expandedSources = if (isExpanded) {
                                expandedSources - balance.id
                            } else {
                                expandedSources + balance.id
                            }
                        },
                        clipboard = clipboard
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { balanceId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            shape = MaterialTheme.shapes.extraLarge,
            iconContentColor = MaterialTheme.colorScheme.error,
            icon = { Icon(Iconax.Danger, contentDescription = null) },
            title = { Text(stringResource(R.string.balance_history_delete_title)) },
            text = { Text(stringResource(R.string.balance_history_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBalance(balanceId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text(stringResource(R.string.accounts_action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(stringResource(R.string.accounts_action_cancel))
                }
            }
        )
    }
}

/** Where a balance record came from, as a tag. */
private data class HistorySource(val icon: ImageVector, val label: String, val color: Color)

@Composable
private fun historySourceOf(balance: AccountBalanceEntity): HistorySource? {
    val scheme = MaterialTheme.colorScheme
    return when (balance.sourceType) {
        "TRANSACTION" -> HistorySource(
            Iconax.Transfer,
            stringResource(R.string.balance_history_source_transaction),
            scheme.tertiary
        )
        "SMS_BALANCE" -> HistorySource(
            Iconax.Messages,
            stringResource(R.string.balance_history_source_sms),
            scheme.secondary
        )
        "CARD_LINK" -> HistorySource(
            Iconax.Card,
            stringResource(R.string.balance_history_source_card_link),
            scheme.primary
        )
        "MANUAL" -> HistorySource(
            Iconax.Edit2,
            stringResource(R.string.balance_history_source_manual),
            scheme.onSurfaceVariant
        )
        else -> if (balance.transactionId != null) {
            HistorySource(
                Iconax.Transfer,
                stringResource(R.string.balance_history_source_transaction),
                scheme.tertiary
            )
        } else {
            null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
private fun BalanceHistoryItem(
    balance: AccountBalanceEntity,
    position: ListItemPosition,
    isLatest: Boolean,
    isOnlyRecord: Boolean,
    isExpanded: Boolean,
    editingId: Long?,
    editingValue: String,
    accountPrimaryCurrency: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditValueChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onToggleExpand: () -> Unit,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    val scheme = MaterialTheme.colorScheme
    val isEditing = editingId == balance.id
    val source = historySourceOf(balance)

    GroupedColumn(
        position = position,
        modifier = Modifier.animateContentSize(),
        // The latest record, and one being edited, sit a step above the rest so
        // the tonal edit field reads as raised.
        containerColor = if (isLatest || isEditing) scheme.surfaceContainer else scheme.surfaceContainerLow,
        verticalArrangement = Arrangement.spacedBy(Spacing.smd)
    ) {
        // Header with date and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Iconax.Clock,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small),
                    tint = scheme.onSurfaceVariant
                )
                Text(
                    text = balance.timestamp.format(
                        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            // Action buttons
            if (!isEditing && !isOnlyRecord) {
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Iconax.Edit2,
                            contentDescription = stringResource(R.string.balance_history_edit_balance),
                            tint = scheme.primary,
                            modifier = Modifier.size(Dimensions.Icon.inline)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Iconax.Bag,
                            contentDescription = stringResource(R.string.balance_history_delete_balance),
                            tint = scheme.error,
                            modifier = Modifier.size(Dimensions.Icon.inline)
                        )
                    }
                }
            }
        }

        // Badges row
        if (isLatest || source != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                // Current badge
                if (isLatest) {
                    Surface(
                        color = scheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = stringResource(R.string.balance_history_current_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimary,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                        )
                    }
                }

                // Source type badge
                if (source != null) {
                    SubtitleTag(
                        text = source.label,
                        color = source.color,
                        textColor = source.color.legibleOn(
                            background = scheme.surfaceContainer,
                            towards = scheme.onSurface
                        ),
                        icon = source.icon
                    )
                }
            }
        }

        // Balance display or edit field
        if (isEditing) {
            // Edit mode
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TonalTextField(
                    value = editingValue,
                    onValueChange = onEditValueChange,
                    label = stringResource(R.string.balance_history_new_balance),
                    prefix = { Text(CurrencyFormatter.getCurrencySymbol(accountPrimaryCurrency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Button(
                        onClick = onSaveEdit,
                        enabled = editingValue.toBigDecimalOrNull() != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.accounts_action_save))
                    }
                    FilledTonalButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.accounts_action_cancel))
                    }
                }
            }
        } else {
            // Display mode - Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.balance_history_balance_label),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(balance.balance, accountPrimaryCurrency),
                    style = PennyWiseText.amountLarge,
                    color = if (isLatest) scheme.primary else scheme.onSurface
                )
            }
        }

        // SMS Source (if available)
        balance.smsSource?.let { smsSource ->
            Surface(
                onClick = onToggleExpand,
                modifier = Modifier.fillMaxWidth(),
                color = scheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.smd)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Iconax.Messages,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.small),
                                    tint = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.balance_history_sms_source),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.onSurfaceVariant
                                )
                                if (!isExpanded) {
                                    Text(
                                        text = pluralStringResource(R.plurals.balance_history_sms_chars, smsSource.length, smsSource.length),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.xs))

                            Text(
                                text = if (isExpanded) smsSource else stringResource(R.string.balance_history_sms_truncated, smsSource.take(SMS_PREVIEW_CHARS)),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isExpanded) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(smsSource))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.balance_history_copy_sms),
                                        modifier = Modifier.size(Dimensions.Icon.small),
                                        tint = scheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) stringResource(R.string.balance_history_collapse_sms) else stringResource(R.string.balance_history_expand_sms),
                                modifier = Modifier.size(Dimensions.Icon.medium),
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
