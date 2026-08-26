package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.LoanStatus
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income_dark
import com.pennywiseai.tracker.ui.theme.income_light
import com.pennywiseai.tracker.ui.theme.loan_dark
import com.pennywiseai.tracker.ui.theme.loan_light
import com.pennywiseai.tracker.utils.Money
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onNavigateBack: () -> Unit = {},
    onLoanClick: (Long) -> Unit = {},
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = uiState.personName,
                hasBackButton = true,
                hasActionButton = false,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + padding.calculateTopPadding(),
                bottom = Dimensions.Padding.content + padding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                PersonBalanceSummary(
                    lent = uiState.lentByCurrency.values.toList(),
                    borrowed = uiState.borrowedByCurrency.values.toList()
                )
            }
            item {
                Text(
                    stringResource(R.string.person_detail_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.loans, key = { it.id }) { loan ->
                PersonLoanRow(loan = loan, onClick = { onLoanClick(loan.id) })
            }
        }
    }
}

@Composable
private fun PersonBalanceSummary(lent: List<Money>, borrowed: List<Money>) {
    val lentColor = if (isSystemInDarkTheme()) loan_dark else loan_light
    val borrowedColor = if (isSystemInDarkTheme()) income_dark else income_light
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(R.string.person_detail_active_balances),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            BalanceLine(stringResource(R.string.person_detail_owed_to_you), lent, lentColor)
            BalanceLine(stringResource(R.string.person_detail_you_owe), borrowed, borrowedColor)
        }
    }
}

@Composable
private fun BalanceLine(label: String, values: List<Money>, color: Color) {
    if (values.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            values.joinToString(" · ") { it.format() },
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun PersonLoanRow(loan: LoanEntity, onClick: () -> Unit) {
    val isLent = loan.direction == LoanDirection.LENT
    val color = if (isLent) {
        if (isSystemInDarkTheme()) loan_dark else loan_light
    } else {
        if (isSystemInDarkTheme()) income_dark else income_light
    }
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (loan.status == LoanStatus.SETTLED) Icons.Default.CheckCircle else Icons.Default.Pending,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    if (isLent) stringResource(R.string.person_detail_owed_to_you)
                    else stringResource(R.string.person_detail_you_owe),
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Text(
                    Money(loan.remainingAmount, loan.currency).format(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    loan.createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (loan.status == LoanStatus.SETTLED) stringResource(R.string.person_detail_settled)
                else stringResource(R.string.person_detail_active),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
