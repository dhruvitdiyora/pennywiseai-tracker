package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.repository.PersonLoanSummary
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.ui.components.AvatarHelper
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.screens.profile.EditProfileSheet
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun PersonalDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    PersonalDashboardContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToContacts = onNavigateToContacts,
        onNavigateToLoans = onNavigateToLoans,
        onNavigateToPerson = onNavigateToPerson,
        onEditProfile = { showEditProfile = true },
        modifier = modifier,
    )
    if (showEditProfile) {
        EditProfileSheet(onDismiss = { showEditProfile = false })
    }
}

@Composable
internal fun PersonalDashboardContent(
    state: PersonalDashboardUiState,
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
) {
    PennyWiseScaffold(
        modifier = modifier,
        title = stringResource(R.string.personal_dashboard_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.people_back))
            }
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@PennyWiseScaffold
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = paddingValues.calculateTopPadding() + Spacing.md,
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
        ) {
            item { DashboardHero(state, onEditProfile) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(title = stringResource(R.string.personal_dashboard_overview))
                    DashboardBalanceRows(state.loanSummary)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(title = stringResource(R.string.personal_dashboard_shortcuts))
                    GroupedList {
                        GroupedRow(position = ListItemPosition.Top, onClick = onNavigateToContacts) {
                            IconTile(
                                Icons.Default.People,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            RowLabels(
                                title = stringResource(R.string.people_contacts_title),
                                subtitle = stringResource(R.string.personal_dashboard_contacts_subtitle, state.people.size),
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.inline))
                        }
                        GroupedRow(position = ListItemPosition.Bottom, onClick = onNavigateToLoans) {
                            IconTile(
                                Icons.Default.SwapHoriz,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            RowLabels(
                                title = stringResource(R.string.lend_borrow_title),
                                subtitle = stringResource(R.string.personal_dashboard_records_subtitle, state.loanSummary.activeLoanCount),
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.inline))
                        }
                    }
                }
            }

            if (state.people.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                        SectionHeaderV2(
                            title = stringResource(R.string.personal_dashboard_people),
                            action = {
                                TextButton(onClick = onNavigateToContacts) {
                                    Text(stringResource(R.string.personal_dashboard_view_all))
                                }
                            },
                        )
                        GroupedList {
                            state.people.take(5).forEachIndexed { index, row ->
                                DashboardPersonRow(
                                    row = row,
                                    position = ListItemPosition.from(index, minOf(state.people.size, 5)),
                                    onClick = { onNavigateToPerson(row.person.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHero(state: PersonalDashboardUiState, onEditProfile: () -> Unit) {
    val background = if (state.profileBackgroundColor != 0) {
        Color(state.profileBackgroundColor)
    } else {
        MaterialTheme.colorScheme.primary
    }
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(Dimensions.Icon.emptyStateContainer),
                shape = CircleShape,
                color = background,
            ) {
                val avatarRes = state.profileImageUri?.let(AvatarHelper::resolveAvatarDrawable)
                when {
                    avatarRes != null -> Image(
                        painter = painterResource(avatarRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    state.profileImageUri != null -> AsyncImage(
                        model = state.profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    else -> Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.userName.dashboardInitials(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColorFor(background),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = state.userName.ifBlank { stringResource(R.string.app_name) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.personal_dashboard_hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            // Edit identity: name, avatar, avatar colour and the Home banner.
            IconButton(onClick = onEditProfile) {
                Icon(
                    imageVector = Iconax.Edit2,
                    contentDescription = stringResource(R.string.edit_profile_title),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                )
            }
        }
    }
}

@Composable
private fun DashboardBalanceRows(summary: PersonLoanSummary) {
    val currencies = (summary.lentByCurrency.keys + summary.borrowedByCurrency.keys).sorted()
    if (currencies.isEmpty()) {
        PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.personal_dashboard_clear),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    GroupedList {
        currencies.forEachIndexed { index, currency ->
            GroupedRow(
                position = ListItemPosition.from(index, currencies.size),
                minHeight = Dimensions.Component.listItemMinHeightTwoLine,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(currency, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DashboardBalanceFigure(
                            stringResource(R.string.home_loans_label_owed_to_you),
                            summary.lentByCurrency[currency] ?: BigDecimal.ZERO,
                            currency,
                            MaterialTheme.colorScheme.income,
                        )
                        DashboardBalanceFigure(
                            stringResource(R.string.home_loans_label_you_owe),
                            summary.borrowedByCurrency[currency] ?: BigDecimal.ZERO,
                            currency,
                            MaterialTheme.colorScheme.expense,
                            true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBalanceFigure(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = PennyWiseText.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(CurrencyFormatter.formatCurrency(amount, currency), style = PennyWiseText.amountMedium, color = color)
    }
}

@Composable
private fun DashboardPersonRow(
    row: PersonWithSummary,
    position: ListItemPosition,
    onClick: () -> Unit,
) {
    val accent = parseProfileColor(row.person.color, MaterialTheme.colorScheme.primary)
    val balance = row.summary.netByCurrency.entries.sortedBy { it.key }.firstOrNull()
    GroupedRow(
        position = position,
        onClick = onClick,
        minHeight = Dimensions.Component.listItemMinHeightTwoLine,
    ) {
        Surface(
            modifier = Modifier.size(Dimensions.Icon.avatarLarge),
            shape = CircleShape,
            color = accent.copy(alpha = Dimensions.Alpha.tonalIconContainer),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(row.person.initials(), color = accent, fontWeight = FontWeight.Bold)
            }
        }
        RowLabels(
            title = row.person.name,
            subtitle = when {
                balance == null -> stringResource(R.string.people_settled_up)
                balance.value.signum() > 0 -> stringResource(
                    R.string.people_owed_to_you_amount,
                    CurrencyFormatter.formatCurrency(balance.value, balance.key),
                )
                else -> stringResource(
                    R.string.people_you_owe_amount,
                    CurrencyFormatter.formatCurrency(balance.value.abs(), balance.key),
                )
            },
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.inline))
    }
}

private fun String.dashboardInitials(): String {
    val words = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        words.isEmpty() -> "PW"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}
