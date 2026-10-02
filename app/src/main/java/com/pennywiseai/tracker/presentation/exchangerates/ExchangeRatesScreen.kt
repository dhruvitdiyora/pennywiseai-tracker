package com.pennywiseai.tracker.presentation.exchangerates

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ExchangeRateEntity
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Convertshape2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.RefreshArrow01
import com.pennywiseai.tracker.ui.screens.rules.UtilityHeroCard
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/*
 * Exchange rates, in the Cashiro style (there is no Cashiro counterpart, so it
 * follows the other utility screens): a large collapsing title with tonal back
 * and refresh buttons, a primary hero card with the last-updated time and the
 * hint, the currency pairs as one connected group, and a tonal "reset all".
 * Rates are never combined or converted here; each row is one pair's rate.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    viewModel: ExchangeRatesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var editingRate by remember { mutableStateOf<ExchangeRateEntity?>(null) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.exchange_rates_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.accounts_back)
                    )
                },
                actionContent = {
                    RefreshButton(
                        enabled = !uiState.isRefreshing,
                        onClick = { viewModel.refreshRates() }
                    )
                },
                hazeState = hazeState
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

            uiState.rates.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    PennyWiseEmptyState(
                        icon = Iconax.Convertshape2,
                        headline = stringResource(R.string.exchange_rates_empty_title),
                        description = stringResource(R.string.exchange_rates_empty_description)
                    )
                }
            }

            else -> {
                val lazyListState = rememberLazyListState()
                val hasCustomRates = uiState.rates.any { it.isCustomRate }
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
                            Spacing.Layout.scrollBottomPadding
                    ),
                    // The rates are one connected group, so the list itself uses the
                    // grouped gutter; the blocks around the group add their own gap.
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
                ) {
                    // Last updated, the hint, and the refreshing indicator
                    item(key = "hero") {
                        UtilityHeroCard(
                            icon = Iconax.Convertshape2,
                            title = uiState.lastUpdated?.let { lastUpdated ->
                                stringResource(
                                    R.string.exchange_rates_last_updated,
                                    lastUpdated.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                                )
                            } ?: stringResource(R.string.exchange_rates_title),
                            body = stringResource(R.string.exchange_rates_hint),
                            modifier = Modifier.padding(bottom = Spacing.md)
                        ) {
                            if (uiState.isRefreshing) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }

                    // Rate rows
                    itemsIndexed(
                        items = uiState.rates,
                        key = { _, rate -> "${rate.fromCurrency}_${rate.toCurrency}" }
                    ) { index, rate ->
                        ExchangeRateRow(
                            rate = rate,
                            position = ListItemPosition.from(index, uiState.rates.size),
                            onClick = { editingRate = rate }
                        )
                    }

                    // Reset button
                    if (hasCustomRates) {
                        item(key = "reset_all") {
                            FilledTonalButton(
                                onClick = { viewModel.clearAllCustomRates() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.md)
                            ) {
                                Icon(
                                    imageVector = Iconax.RefreshArrow01,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimensions.Icon.small)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(stringResource(R.string.exchange_rates_reset_all))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit rate dialog
    editingRate?.let { rate ->
        EditRateDialog(
            rate = rate,
            onDismiss = { editingRate = null },
            onSetCustomRate = { newRate ->
                viewModel.setCustomRate(rate.fromCurrency, rate.toCurrency, newRate)
                editingRate = null
            },
            onResetToAuto = {
                viewModel.clearCustomRate(rate.fromCurrency, rate.toCurrency)
                editingRate = null
            }
        )
    }
}

/** The round, tonal refresh button of the top bar; disabled while a refresh runs. */
@Composable
private fun RefreshButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(end = Dimensions.Padding.content),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = scheme.surfaceContainer,
            contentColor = scheme.onBackground,
            disabledContainerColor = scheme.surfaceContainer,
            disabledContentColor = scheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Iconax.RefreshArrow01,
            contentDescription = stringResource(R.string.exchange_rates_refresh),
            modifier = Modifier.size(Dimensions.Icon.inline)
        )
    }
}

/**
 * One currency pair: "$ USD → ₹ INR" on the left, the rate on the right with a
 * tag saying whether it came from the API or was set by hand. The row opens the
 * custom-rate dialog.
 */
@Composable
private fun ExchangeRateRow(
    rate: ExchangeRateEntity,
    position: ListItemPosition,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    GroupedRow(
        position = position,
        onClick = onClick,
        minHeight = Dimensions.Component.listItemMinHeight
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${CurrencyFormatter.getCurrencySymbol(rate.fromCurrency)} ${rate.fromCurrency}",
                style = PennyWiseText.rowTitle,
                color = scheme.onSurface
            )
            // Mirrors in RTL, unlike the arrow character it replaces.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = scheme.onSurfaceVariant
            )
            Text(
                text = "${CurrencyFormatter.getCurrencySymbol(rate.toCurrency)} ${rate.toCurrency}",
                style = PennyWiseText.rowTitle,
                color = scheme.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(
                text = rate.rate.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString(),
                style = PennyWiseText.amountRow,
                color = scheme.onSurface
            )
            if (rate.isCustomRate) {
                SubtitleTag(
                    text = stringResource(R.string.exchange_rates_badge_custom),
                    color = scheme.primary
                )
            } else {
                SubtitleTag(
                    text = stringResource(R.string.exchange_rates_badge_api),
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditRateDialog(
    rate: ExchangeRateEntity,
    onDismiss: () -> Unit,
    onSetCustomRate: (BigDecimal) -> Unit,
    onResetToAuto: () -> Unit
) {
    var rateText by remember {
        mutableStateOf(rate.rate.setScale(6, java.math.RoundingMode.HALF_UP).toPlainString())
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.Convertshape2, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    R.string.utility_exchange_rates_pair,
                    rate.fromCurrency,
                    rate.toCurrency
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(
                        R.string.utility_exchange_rates_prompt,
                        rate.fromCurrency,
                        rate.toCurrency
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TonalTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        isError = it.toBigDecimalOrNull() == null || (it.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO
                    },
                    label = stringResource(R.string.exchange_rates_rate_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    supportingText = if (isError) {
                        stringResource(R.string.exchange_rates_invalid_rate)
                    } else {
                        null
                    }
                )

                if (rate.isCustomRate) {
                    TextButton(
                        onClick = onResetToAuto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.exchange_rates_reset),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRate = rateText.toBigDecimalOrNull()
                    if (newRate != null && newRate > BigDecimal.ZERO) {
                        onSetCustomRate(newRate)
                    }
                },
                enabled = !isError && rateText.isNotBlank()
            ) {
                Text(stringResource(R.string.exchange_rates_set_custom))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accounts_action_cancel))
            }
        }
    )
}
