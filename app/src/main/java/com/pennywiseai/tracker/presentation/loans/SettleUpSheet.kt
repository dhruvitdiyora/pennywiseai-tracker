package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.people.PersonAvatar
import com.pennywiseai.tracker.presentation.people.initialsOf
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

/**
 * Confirms settling up with [person]: what is still open (one line per
 * currency, never added together), a note on what settling does, and the
 * confirm action. Settling closes every open loan with the person; anything
 * unpaid is forgiven, exactly as the old confirmation dialog said.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettleUpSheet(
    person: LoanPerson,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val balances = person.net.values.toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PersonAvatar(
                initials = initialsOf(person.name),
                color = person.net.values.firstOrNull()?.let { loanBalanceColor(it) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant,
                size = Dimensions.Icon.avatarLarge,
                tinted = true,
            )
            Text(
                text = stringResource(R.string.loans_settle_up_title, person.name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = Dimensions.Alpha.medium),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.smd),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Iconax.Information,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.loans_settle_up_message, person.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.loans_settle_up_sheet_balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (balances.isEmpty()) {
                    // Open loans in both directions cancel out exactly.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            text = stringResource(R.string.loans_person_even),
                            modifier = Modifier.padding(Spacing.md),
                            style = PennyWiseText.rowSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    GroupedList {
                        balances.forEachIndexed { index, money ->
                            GroupedRow(position = ListItemPosition.from(index, balances.size)) {
                                Text(
                                    text = stringResource(
                                        if (money.isOwedToYou) {
                                            R.string.loans_person_owes_you
                                        } else {
                                            R.string.loans_person_you_owe
                                        },
                                        CurrencyFormatter.formatCurrency(money.amount.abs(), money.currency),
                                    ),
                                    style = PennyWiseText.amountRow,
                                    color = loanBalanceColor(money),
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimensions.Component.fab),
            ) {
                Text(
                    text = stringResource(R.string.loans_settle_up),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    }
}
