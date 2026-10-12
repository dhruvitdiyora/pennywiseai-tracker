package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

/**
 * Confirms settling up with [person], laid out like Cashiro's SettleUpSheet:
 * the centred title, an info card on what settling does, what is still open
 * (one grouped row per currency, never added together), and the tall confirm
 * button. Settling closes every open loan with the person; anything unpaid is
 * forgiven, exactly as the old confirmation dialog said.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettleUpSheet(
    person: LoanPerson,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    contact: LoanContactStyle? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val balances = person.net.values.toList()

    PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            LoanPersonAvatar(
                name = person.name,
                contact = contact,
                accent = person.accentColor(),
                size = Dimensions.Icon.avatarLarge,
            )
            LoanSheetTitle(title = stringResource(R.string.loans_settle_up_title, person.name))

            LoanInfoCard(
                title = stringResource(R.string.loans_settle_up),
                text = stringResource(R.string.loans_settle_up_message, person.name),
                modifier = Modifier.padding(top = Spacing.xs),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.loans_settle_up_sheet_balance),
                    modifier = Modifier.padding(start = Spacing.xs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GroupedList {
                    if (balances.isEmpty()) {
                        // Open loans in both directions cancel out exactly.
                        GroupedRow(position = ListItemPosition.Single) {
                            Text(
                                text = stringResource(R.string.loans_person_even),
                                style = PennyWiseText.rowSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = loanBalanceColor(money),
                                )
                            }
                        }
                    }
                }
            }

            LoanSheetButton(
                label = stringResource(R.string.loans_settle_up),
                onClick = onConfirm,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}
