package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income_dark
import com.pennywiseai.tracker.ui.theme.income_light
import com.pennywiseai.tracker.ui.theme.loan_dark
import com.pennywiseai.tracker.ui.theme.loan_light
import com.pennywiseai.tracker.utils.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowScreen(
    onNavigateBack: () -> Unit = {},
    onPersonClick: (String) -> Unit = {},
    onAdd: () -> Unit = {},
    viewModel: LendBorrowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.lend_borrow_title),
                hasBackButton = true,
                hasActionButton = false,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.lend_borrow_add)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.people.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                PennyWiseEmptyState(
                    icon = Icons.Default.SwapHoriz,
                    headline = stringResource(R.string.lend_borrow_empty_title),
                    description = stringResource(R.string.lend_borrow_empty_body),
                    actionLabel = stringResource(R.string.lend_borrow_add),
                    onAction = onAdd
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + padding.calculateTopPadding(),
                    bottom = Dimensions.Padding.content + padding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(uiState.people, key = { it.personId }) { person ->
                    PersonSummaryCard(person = person, onClick = { onPersonClick(person.personId) })
                }
            }
        }
    }
}

@Composable
private fun PersonSummaryCard(person: PersonSummary, onClick: () -> Unit) {
    val lentColor = if (androidx.compose.foundation.isSystemInDarkTheme()) loan_dark else loan_light
    val borrowedColor = if (androidx.compose.foundation.isSystemInDarkTheme()) income_dark else income_light
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Text(person.personName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(person.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LoanBalanceCard(
                    label = stringResource(R.string.lend_borrow_owed_to_you),
                    values = person.lentByCurrency.values.toList(),
                    color = lentColor
                )
                LoanBalanceCard(
                    label = stringResource(R.string.lend_borrow_you_owe),
                    values = person.borrowedByCurrency.values.toList(),
                    color = borrowedColor
                )
            }
        }
    }
}

@Composable
private fun LoanBalanceCard(label: String, values: List<Money>, color: Color) {
    if (values.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            values.joinToString(" · ") { it.format() },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
