package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BalanceChart
import com.pennywiseai.tracker.ui.components.BalancePoint
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

/** A glance-level history view from the account-card overflow menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceHistorySheet(
    bankName: String,
    accountLast4: String,
    onDismiss: () -> Unit,
    viewModel: BalanceHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(bankName, accountLast4) { viewModel.loadAccount(bankName, accountLast4) }
    val history = viewModel.history.collectAsStateWithLifecycle().value
    val currency = history.firstOrNull()?.let {
        CurrencyFormatter.resolveAccountCurrency(it.sourceType, it.currency, bankName)
    } ?: "INR"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                bottom = Dimensions.Padding.content
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Balance history", style = MaterialTheme.typography.titleLarge)
                    Text(
                        AccountBalanceEntity.accountLabel(bankName, accountLast4),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (history.isNotEmpty()) {
                item {
                    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                        BalanceChart(
                            primaryCurrency = currency,
                            balanceHistory = history.map {
                                BalancePoint(it.timestamp, it.balance, currency)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            height = 160
                        )
                    }
                }
                items(history, key = { it.id }) { record ->
                    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                CurrencyFormatter.formatCurrency(record.balance, currency),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                record.timestamp.format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "No balance history yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
