package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.TransactionGroupEntity
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.icons.iconax.Folder2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.loan_dark
import com.pennywiseai.tracker.ui.theme.loan_light
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

/*
 * The two bottom sheets of the Transaction Detail screen. Both sit on the page
 * surface (one step below the cards), so the tonal fields and rows inside them
 * read as cards — the same convention as the Lend & Borrow sheets.
 */

// ── Mark as loan ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TxnDetailMarkAsLoanSheet(
    transactionAmount: BigDecimal,
    transactionCurrency: String,
    direction: LoanDirection,
    recentPersonNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (personName: String, note: String?, loanAmount: BigDecimal?) -> Unit,
) {
    var personName by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(recentPersonNames.isEmpty()) }
    var note by remember { mutableStateOf("") }
    // Pre-filled with the full transaction amount — user only edits this when
    // a portion of the payment isn't part of the loan (e.g. issue #309: paid
    // ₹5500 but only ₹3500 is meant to come back).
    var loanAmountInput by remember(transactionAmount) {
        mutableStateOf(transactionAmount.toPlainString())
    }
    val parsedLoanAmount = loanAmountInput.toBigDecimalOrNull()
    val isLoanAmountValid = parsedLoanAmount != null &&
        parsedLoanAmount > BigDecimal.ZERO &&
        parsedLoanAmount <= transactionAmount

    val scheme = MaterialTheme.colorScheme
    val loanColor = if (isSystemInDarkTheme()) loan_dark else loan_light
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Scroll so the loan-amount/note fields and the Confirm button stay
                // reachable when several existing people are listed — otherwise they're
                // pushed below the sheet and the user can only proceed via "New person".
                // (#489)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimensions.Padding.dialog)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = stringResource(
                        if (direction == LoanDirection.LENT) R.string.txn_detail_loan_sheet_lent
                        else R.string.txn_detail_loan_sheet_borrowed,
                        CurrencyFormatter.formatCurrency(transactionAmount, transactionCurrency)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        if (direction == LoanDirection.LENT) R.string.txn_detail_loan_sheet_who_lent
                        else R.string.txn_detail_loan_sheet_who_borrowed
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (!isAddingNew && recentPersonNames.isNotEmpty()) {
                // Pick from existing people
                GroupedList {
                    recentPersonNames.forEachIndexed { index, name ->
                        val isSelected = personName == name
                        GroupedRow(
                            position = ListItemPosition.from(index, recentPersonNames.size),
                            onClick = { personName = name },
                            containerColor = if (isSelected) {
                                loanColor.copy(alpha = SELECTED_ROW_ALPHA).compositeOver(scheme.surfaceContainerLow)
                            } else {
                                scheme.surfaceContainerLow
                            },
                            modifier = Modifier.semantics { selected = isSelected }
                        ) {
                            PersonAvatar(
                                initials = initialsOf(name),
                                color = loanColor,
                                tinted = true
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(Dimensions.Icon.inline)
                                )
                            }
                        }
                    }
                }

                // Add new person option
                TextButton(
                    onClick = {
                        isAddingNew = true
                        personName = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.txn_detail_loan_new_person))
                }
            } else {
                // Text field for new person name
                TonalTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = stringResource(R.string.txn_detail_loan_person_name)
                )
                if (recentPersonNames.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            isAddingNew = false
                            personName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.txn_detail_loan_pick_existing))
                    }
                }
            }

            // Loan amount (defaults to full txn amount; user lowers when only a
            // portion of the payment is meant to come back).
            TonalTextField(
                value = loanAmountInput,
                onValueChange = { loanAmountInput = it },
                label = stringResource(R.string.txn_detail_loan_amount),
                supportingText = if (parsedLoanAmount != null && parsedLoanAmount < transactionAmount) {
                    stringResource(R.string.txn_detail_loan_amount_partial_hint)
                } else {
                    stringResource(
                        R.string.txn_detail_loan_amount_max,
                        CurrencyFormatter.formatCurrency(transactionAmount, transactionCurrency)
                    )
                },
                isError = !isLoanAmountValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Optional note
            TonalTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.txn_detail_loan_note)
            )

            // Confirm button
            Button(
                onClick = {
                    onConfirm(
                        personName.trim(),
                        note.trim().ifEmpty { null },
                        parsedLoanAmount
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.minTouchTarget),
                enabled = personName.isNotBlank() && isLoanAmountValid
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.txn_detail_action_confirm))
            }
        }
    }
}

/** How strongly the loan hue tints the selected person's row. */
private const val SELECTED_ROW_ALPHA = 0.16f

// ── Transaction group ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TxnDetailGroupSheet(
    currentGroup: TransactionGroupEntity?,
    availableGroups: List<TransactionGroupEntity>,
    onDismiss: () -> Unit,
    onAddToGroup: (Long) -> Unit,
    onRemoveFromGroup: () -> Unit,
    onCreateGroup: (String, String?) -> Unit,
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimensions.Padding.dialog)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.txn_detail_group_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (currentGroup != null) {
                GroupedList {
                    GroupedRow(position = ListItemPosition.Single) {
                        TxnGroupIcon()
                        RowLabels(
                            title = stringResource(R.string.txn_detail_group_current, currentGroup.name)
                        )
                        TextButton(onClick = {
                            onRemoveFromGroup()
                            onDismiss()
                        }) {
                            Text(
                                text = stringResource(R.string.txn_detail_group_remove),
                                color = scheme.error
                            )
                        }
                    }
                }
            }

            val otherGroups = availableGroups.filter { it.id != currentGroup?.id }
            if (otherGroups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = stringResource(
                            if (currentGroup != null) R.string.txn_detail_group_move_to
                            else R.string.txn_detail_menu_add_to_group
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    GroupedList {
                        otherGroups.forEachIndexed { index, group ->
                            GroupedRow(
                                position = ListItemPosition.from(index, otherGroups.size),
                                onClick = { onAddToGroup(group.id) }
                            ) {
                                TxnGroupIcon()
                                RowLabels(title = group.name)
                            }
                        }
                    }
                }
            }

            if (showCreateField) {
                TonalTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = stringResource(R.string.txn_detail_group_name),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateGroup(newGroupName.trim(), null)
                                }
                            },
                            enabled = newGroupName.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.txn_detail_group_create)
                            )
                        }
                    }
                )
            } else {
                FilledTonalButton(
                    onClick = { showCreateField = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.Component.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.txn_detail_group_create_new))
                }
            }
        }
    }
}

@Composable
private fun TxnGroupIcon() {
    val scheme = MaterialTheme.colorScheme
    IconTile(
        icon = Iconax.Folder2,
        containerColor = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        size = Dimensions.Icon.list,
        glyphSize = Dimensions.Icon.inline
    )
}
