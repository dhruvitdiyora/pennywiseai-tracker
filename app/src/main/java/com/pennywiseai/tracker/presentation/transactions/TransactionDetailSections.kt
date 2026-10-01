package com.pennywiseai.tracker.presentation.transactions

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.PreferenceSwitch
import com.pennywiseai.tracker.ui.components.SplitItem
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Category2
import com.pennywiseai.tracker.ui.icons.iconax.Chart2
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.ReceiptItem
import com.pennywiseai.tracker.ui.icons.iconax.RefreshCircle
import com.pennywiseai.tracker.ui.icons.iconax.Transfer
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.icons.iconax.WalletMoney
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.loan_dark
import com.pennywiseai.tracker.ui.theme.loan_light
import com.pennywiseai.tracker.ui.theme.warning
import com.pennywiseai.tracker.utils.BalanceDiscrepancy
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/*
 * The read-only body of the Transaction Detail screen, Cashiro-style: the hero
 * card, then rounded info rows grouped into connected blocks (details, then the
 * loan / analytics switches), then the receipt, the original SMS and the split
 * breakdown. The shape does the grouping — no dividers.
 */

/** One row of a connected block; [content] gets its slot so the corners come out right. */
private class TxnDetailEntry(val content: @Composable (ListItemPosition) -> Unit)

@Composable
internal fun TxnDetailReadOnlyBody(
    transaction: TransactionEntity,
    primaryCurrency: String,
    convertedAmount: BigDecimal?,
    viewModel: TransactionDetailViewModel,
    splits: List<SplitItem>,
    hasSplits: Boolean,
    loan: LoanEntity?,
    onNavigateToLoanDetail: (Long) -> Unit,
    onUnmarkLoanClick: () -> Unit,
    accountProfileId: Long?,
    modifier: Modifier = Modifier,
) {
    val currentAlias by viewModel.currentMerchantAlias.collectAsStateWithLifecycle()
    val detailTags by viewModel.transactionTags.collectAsStateWithLifecycle()
    val discrepancy by viewModel.balanceDiscrepancy.collectAsStateWithLifecycle()
    val addingAdjustment by viewModel.isAddingAdjustment.collectAsStateWithLifecycle()
    val receiptUri by viewModel.receiptUri.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        TxnDetailHero(
            transaction = transaction,
            merchantTitle = txnDetailMerchantTitle(transaction, currentAlias),
            primaryCurrency = primaryCurrency,
            convertedAmount = convertedAmount
        )

        TxnDetailInfoGroup(
            transaction = transaction,
            primaryCurrency = primaryCurrency,
            splits = splits,
            hasSplits = hasSplits,
            tags = detailTags,
            accountProfileId = accountProfileId
        )

        // Bank balance vs. ledger prediction (#734/#135): surface the gap and
        // offer to record it as an untracked transaction.
        discrepancy?.let { gap ->
            TxnDetailMismatchCard(
                discrepancy = gap,
                adding = addingAdjustment,
                onAdd = { viewModel.addBalanceAdjustment() }
            )
        }

        GroupedList {
            TxnDetailLoanRow(
                position = ListItemPosition.Top,
                loan = loan,
                onMark = { viewModel.showMarkAsLoanSheet() },
                onOpen = onNavigateToLoanDetail,
                onUnmark = onUnmarkLoanClick
            )
            // Exclude from analytics (#451). The switch carries its own
            // semantics; the row tap is the single toggle handler.
            PreferenceSwitch(
                title = stringResource(R.string.txn_detail_exclude_analytics_title),
                subtitle = stringResource(R.string.txn_detail_exclude_analytics_subtitle),
                checked = transaction.excludedFromAnalytics,
                onCheckedChange = { viewModel.setExcludedFromAnalytics(it) },
                position = ListItemPosition.Bottom,
                leadingIcon = { TxnDetailIcon(Iconax.Chart2) }
            )
        }

        receiptUri?.let { uri ->
            ReceiptAttachmentCard(
                model = uri,
                onView = viewModel::showFullScreenReceipt,
                onShare = {
                    val shareUri = viewModel.getReceiptShareUri()
                    if (shareUri == null) {
                        Toast.makeText(context, R.string.receipt_unavailable, Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    buildReceiptShareIntent(shareUri, transaction.merchantName),
                                    context.getString(R.string.share_receipt),
                                )
                            )
                        }.onFailure {
                            Toast.makeText(context, R.string.receipt_share_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }

        if (!transaction.smsBody.isNullOrBlank()) {
            TxnDetailSmsSection(smsBody = transaction.smsBody)
        }

        if (hasSplits && splits.isNotEmpty()) {
            TxnDetailSplitBreakdown(splits = splits, currency = transaction.currency)
        }
    }
}

// ── Info rows ─────────────────────────────────────────────────────────────

private fun maskAccount(value: String): String =
    if (value.length > 4) "*".repeat(value.length - 4) + value.takeLast(4) else value

@Composable
private fun TxnDetailInfoGroup(
    transaction: TransactionEntity,
    primaryCurrency: String,
    splits: List<SplitItem>,
    hasSplits: Boolean,
    tags: List<String>,
    accountProfileId: Long?,
) {
    val dateText = transaction.dateTime.format(
        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy · h:mm a")
    )
    val isSplit = hasSplits && splits.isNotEmpty()
    val bankName = transaction.bankName
    val description = transaction.description
    val fromAccount = transaction.fromAccount
    val toAccount = transaction.toAccount
    val accountNumber = transaction.accountNumber
    val balanceAfter = transaction.balanceAfter
    val reference = transaction.reference
    val smsSender = transaction.smsSender
    val effectiveProfileId = transaction.profileId ?: accountProfileId
    val isBusiness = effectiveProfileId == ProfileEntity.BUSINESS_ID

    val entries = buildList<TxnDetailEntry> {
        // Date & time
        add(TxnDetailEntry { position ->
            TxnDetailRow(
                position = position,
                label = stringResource(R.string.txn_detail_label_date_time),
                value = dateText,
                leading = { TxnDetailIcon(Iconax.Calendar) }
            )
        })

        // Category (or the number of categories a split spreads over)
        add(TxnDetailEntry { position ->
            TxnDetailRow(
                position = position,
                label = stringResource(R.string.txn_detail_label_category),
                value = if (isSplit) {
                    pluralStringResource(R.plurals.txn_detail_split_categories, splits.size, splits.size)
                } else {
                    transaction.category
                },
                leading = {
                    if (isSplit) {
                        TxnDetailIcon(Iconax.Category2)
                    } else {
                        TxnDetailCategoryTile(transaction.category)
                    }
                }
            )
        })

        // Bank
        if (bankName != null) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_label_bank),
                    value = bankName,
                    leading = {
                        // A bank we have no logo for would borrow an unrelated
                        // category glyph from BrandIcon, so fall back to a wallet.
                        if (BrandIcons.getIconResource(bankName) != null) {
                            BrandIcon(
                                merchantName = bankName,
                                size = Dimensions.Icon.list,
                                showBackground = true
                            )
                        } else {
                            TxnDetailIcon(Iconax.Wallet3)
                        }
                    }
                )
            })
        }

        // Account(s)
        if (fromAccount != null && toAccount != null) {
            add(TxnDetailEntry { position ->
                TxnDetailTransferRow(
                    position = position,
                    fromValue = maskAccount(fromAccount),
                    toValue = maskAccount(toAccount)
                )
            })
        } else {
            if (accountNumber != null && fromAccount == null && toAccount == null) {
                add(TxnDetailEntry { position ->
                    TxnDetailRow(
                        position = position,
                        label = stringResource(R.string.txn_detail_label_account),
                        value = maskAccount(accountNumber),
                        leading = { TxnDetailIcon(Iconax.Wallet3) }
                    )
                })
            }
            if (fromAccount != null) {
                add(TxnDetailEntry { position ->
                    TxnDetailRow(
                        position = position,
                        label = stringResource(R.string.txn_detail_label_from),
                        value = maskAccount(fromAccount),
                        leading = { TxnDetailIcon(Iconax.Wallet3) }
                    )
                })
            }
            if (toAccount != null) {
                add(TxnDetailEntry { position ->
                    TxnDetailRow(
                        position = position,
                        label = stringResource(R.string.txn_detail_label_to),
                        value = maskAccount(toAccount),
                        leading = { TxnDetailIcon(Iconax.Wallet3) }
                    )
                })
            }
        }

        // Balance after the transaction, in the account's currency as before.
        if (balanceAfter != null) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_label_balance),
                    value = CurrencyFormatter.formatCurrency(balanceAfter, primaryCurrency),
                    leading = { TxnDetailIcon(Iconax.WalletMoney) }
                )
            })
        }

        // Notes
        if (description != null) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_label_description),
                    value = description,
                    leading = { TxnDetailIcon(Iconax.DocumentText2) }
                )
            })
        }

        // Tags
        if (tags.isNotEmpty()) {
            add(TxnDetailEntry { position ->
                TxnDetailTagsRow(position = position, tags = tags)
            })
        }

        // Recurring
        if (transaction.isRecurring) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_label_status),
                    value = stringResource(R.string.txn_detail_status_recurring),
                    leading = { TxnDetailIcon(Iconax.RefreshCircle) }
                )
            })
        }

        // Classification
        add(TxnDetailEntry { position ->
            TxnDetailRow(
                position = position,
                label = stringResource(R.string.txn_detail_label_classification),
                value = stringResource(
                    if (isBusiness) R.string.txn_detail_business else R.string.txn_detail_personal
                ),
                leading = {
                    TxnDetailIcon(if (isBusiness) Icons.Default.Business else Icons.Default.Person)
                }
            )
        })

        // Reference number, and the sender id the SMS came from.
        if (reference != null) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_label_reference),
                    value = reference,
                    leading = { TxnDetailIcon(Iconax.ReceiptItem) }
                )
            })
        }
        if (smsSender != null) {
            add(TxnDetailEntry { position ->
                TxnDetailRow(
                    position = position,
                    label = stringResource(R.string.txn_detail_ui_label_sms_sender),
                    value = smsSender,
                    leading = { TxnDetailIcon(Iconax.Messages) }
                )
            })
        }
    }

    GroupedList {
        entries.forEachIndexed { index, entry ->
            entry.content(ListItemPosition.from(index, entries.size))
        }
    }
}

/** The neutral leading tile of an info row. */
@Composable
private fun TxnDetailIcon(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    IconTile(
        icon = icon,
        containerColor = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        size = Dimensions.Icon.list,
        glyphSize = Dimensions.Icon.inline
    )
}

/** The category's own colour at low alpha, with its glyph (or the user's emoji) inside. */
@Composable
private fun TxnDetailCategoryTile(category: String) {
    val color = CategoryMapping.colorFor(category)
    Box(
        modifier = Modifier
            .size(Dimensions.Icon.list)
            .clip(CircleShape)
            .background(color.copy(alpha = Dimensions.Alpha.tonalIconContainer)),
        contentAlignment = Alignment.Center
    ) {
        CategoryIcon(category = category, size = Dimensions.Icon.inline)
    }
}

/** A label above its value, with a leading tile — the building block of the details block. */
@Composable
private fun TxnDetailRow(
    position: ListItemPosition,
    label: String,
    value: String,
    leading: @Composable () -> Unit,
) {
    TxnDetailRowFrame(position = position, label = label, leading = leading) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TxnDetailRowFrame(
    position: ListItemPosition,
    label: String,
    leading: @Composable () -> Unit,
    value: @Composable ColumnScope.() -> Unit,
) {
    GroupedRow(
        position = position,
        // Read "Date & Time, Fri, Oct 2" as one stop instead of two.
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        leading()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(
                text = label,
                style = PennyWiseText.fieldLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            value()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TxnDetailTagsRow(position: ListItemPosition, tags: List<String>) {
    TxnDetailRowFrame(
        position = position,
        label = pluralStringResource(R.plurals.txn_detail_label_tags, tags.size),
        leading = { TxnDetailIcon(Icons.Default.Sell) }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            tags.forEach { tag ->
                SubtitleTag(text = tag, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** A transfer reads best as one row: [from] -> [to]. */
@Composable
private fun TxnDetailTransferRow(
    position: ListItemPosition,
    fromValue: String,
    toValue: String,
) {
    GroupedRow(
        position = position,
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        TxnDetailIcon(Iconax.Transfer)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TxnDetailAccountPill(text = fromValue, modifier = Modifier.weight(1f, fill = false))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TxnDetailAccountPill(text = toValue, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun TxnDetailAccountPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.smd, vertical = Spacing.sm)
        )
    }
}

// ── Loan + mismatch ───────────────────────────────────────────────────────

/**
 * The loan this transaction is linked to (tap to open it, or unmark it), or an
 * invitation to mark the transaction as a loan.
 */
@Composable
private fun TxnDetailLoanRow(
    position: ListItemPosition,
    loan: LoanEntity?,
    onMark: () -> Unit,
    onOpen: (Long) -> Unit,
    onUnmark: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    if (loan != null) {
        val loanColor = if (isSystemInDarkTheme()) loan_dark else loan_light
        GroupedRow(position = position, onClick = { onOpen(loan.id) }) {
            IconTile(
                icon = Icons.Default.SwapHoriz,
                containerColor = loanColor.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                contentColor = loanColor,
                size = Dimensions.Icon.list,
                glyphSize = Dimensions.Icon.inline
            )
            RowLabels(
                title = if (loan.direction == LoanDirection.LENT) {
                    stringResource(R.string.txn_detail_loan_lent_to, loan.personName)
                } else {
                    stringResource(R.string.txn_detail_loan_borrowed_from, loan.personName)
                },
                subtitle = stringResource(R.string.txn_detail_ui_loan_view)
            )
            // Unmark-as-loan (#444): removes the loan link; deletes the loan
            // too if no other transactions are linked.
            IconButton(onClick = onUnmark) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.txn_detail_unmark_loan),
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
    } else {
        GroupedRow(position = position, onClick = onMark) {
            TxnDetailIcon(Icons.Default.SwapHoriz)
            RowLabels(
                title = stringResource(R.string.txn_detail_mark_as_loan),
                subtitle = stringResource(R.string.txn_detail_ui_loan_mark_hint)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.medium),
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TxnDetailMismatchCard(
    discrepancy: BalanceDiscrepancy,
    adding: Boolean,
    onAdd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val warningColor = scheme.warning
    GroupedColumn(position = ListItemPosition.Single) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            IconTile(
                icon = Iconax.Danger,
                containerColor = warningColor.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                contentColor = warningColor,
                size = Dimensions.Icon.list,
                glyphSize = Dimensions.Icon.inline
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.txn_detail_label_balance_mismatch),
                    style = PennyWiseText.fieldLabel,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.txn_detail_balance_mismatch_value,
                        CurrencyFormatter.formatCurrency(discrepancy.delta.abs(), discrepancy.currency),
                        CurrencyFormatter.formatCurrency(discrepancy.expected, discrepancy.currency)
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )
            }
        }
        FilledTonalButton(
            onClick = onAdd,
            enabled = !adding,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.minTouchTarget)
        ) {
            Text(
                text = stringResource(
                    if (discrepancy.delta.signum() < 0) R.string.txn_detail_add_untracked_expense
                    else R.string.txn_detail_add_untracked_income,
                    CurrencyFormatter.formatCurrency(discrepancy.delta.abs(), discrepancy.currency)
                )
            )
        }
    }
}

// ── SMS + splits ──────────────────────────────────────────────────────────

/** The original SMS: a single tonal row that unfolds into the monospaced message. */
@Composable
internal fun TxnDetailSmsSection(smsBody: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_EXPANDED_DEGREES else 0f,
        label = "smsChevron"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ListItemPosition.Single.toShape(),
        color = scheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
                    .padding(
                        horizontal = Spacing.md,
                        vertical = Dimensions.Padding.listRowVertical
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                TxnDetailIcon(Iconax.Messages)
                Text(
                    text = stringResource(
                        if (expanded) R.string.txn_detail_sms_hide else R.string.txn_detail_sms_show
                    ),
                    style = PennyWiseText.rowTitle,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimensions.Icon.medium)
                        .rotate(chevronRotation),
                    tint = scheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                    shape = MaterialTheme.shapes.medium,
                    color = scheme.surfaceContainerHigh
                ) {
                    Text(
                        text = smsBody,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = scheme.onSurface,
                        modifier = Modifier.padding(Spacing.smd)
                    )
                }
            }
        }
    }
}

private const val CHEVRON_EXPANDED_DEGREES = 180f

/** Read-only split breakdown: one connected row per category with its share. */
@Composable
private fun TxnDetailSplitBreakdown(splits: List<SplitItem>, currency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
        SectionHeaderV2(
            title = stringResource(R.string.split_breakdown_title),
            topSpacing = Spacing.none
        )
        GroupedList {
            splits.forEachIndexed { index, split ->
                GroupedRow(
                    position = ListItemPosition.from(index, splits.size),
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    TxnDetailCategoryTile(split.category)
                    Text(
                        text = split.category,
                        style = PennyWiseText.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(split.amount, currency),
                        style = PennyWiseText.amountRow,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
