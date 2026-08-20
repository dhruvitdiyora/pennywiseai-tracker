package com.pennywiseai.tracker.presentation.more

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * The More hub.
 *
 * Everything that isn't Home or Analytics lives here, as tiles that carry their own live
 * status. Before this screen existed, Subscriptions, Loans, Budgets, Categories, Rules,
 * Groups, Import and Exchange Rates were reachable only via Settings → Data Management —
 * four levels down from Home for features that were fully built.
 *
 * A tile states what's inside before you tap it ("8 active", "3 need review"), which is what
 * makes a hub worth visiting rather than just a menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MoreViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToImportStatement: () -> Unit = {},
    onNavigateToExchangeRates: () -> Unit = {},
    onNavigateToHomePanels: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = "More",
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = Dimensions.Component.bottomBarHeight + Spacing.Layout.scrollBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap)
        ) {
            item {
                MoreSection(
                    title = "Money",
                    topSpacing = Spacing.none,
                    tiles = listOf(
                        MoreTileSpec(
                            Icons.Default.Subscriptions, "Subscriptions",
                            uiState.activeSubscriptions.countStatus("active"),
                            onNavigateToSubscriptions
                        ),
                        MoreTileSpec(
                            Icons.Default.SwapHoriz, "Loans",
                            uiState.activeLoans.countStatus("active"),
                            onNavigateToLoans
                        ),
                        MoreTileSpec(
                            Icons.Default.PieChart, "Budgets",
                            uiState.budgetGroups.countStatus("set"),
                            onNavigateToBudgets
                        ),
                        MoreTileSpec(
                            Icons.Default.AccountBalance, "Accounts",
                            uiState.accounts.countStatus("tracked"),
                            onNavigateToAccounts
                        ),
                    )
                )
            }

            item {
                MoreSection(
                    title = "Organise",
                    tiles = listOf(
                        MoreTileSpec(
                            Icons.Default.Category, "Categories",
                            uiState.categories.countStatus(""),
                            onNavigateToCategories
                        ),
                        MoreTileSpec(
                            Icons.Default.AutoAwesome, "Smart Rules",
                            uiState.rules.countStatus("active"),
                            onNavigateToRules
                        ),
                        MoreTileSpec(
                            Icons.Default.FolderOpen, "Groups",
                            uiState.transactionGroups.countStatus(""),
                            onNavigateToGroups
                        ),
                        MoreTileSpec(
                            Icons.Default.Sms, "Unrecognised SMS",
                            uiState.unreviewedSms.countStatus("to review"),
                            onNavigateToUnrecognizedSms
                        ),
                    )
                )
            }

            item {
                MoreSection(
                    title = "Data",
                    tiles = listOf(
                        MoreTileSpec(
                            Icons.Default.UploadFile, "Import statement",
                            null, onNavigateToImportStatement
                        ),
                        MoreTileSpec(
                            Icons.Default.CurrencyExchange, "Exchange rates",
                            null, onNavigateToExchangeRates
                        ),
                    )
                )
            }

            item {
                MoreSection(
                    title = "App",
                    tiles = listOf(
                        MoreTileSpec(
                            Icons.AutoMirrored.Filled.Chat, "PennyWise AI",
                            null, onNavigateToChat
                        ),
                        MoreTileSpec(
                            Icons.Default.Settings, "Settings",
                            null, onNavigateToSettings
                        ),
                        MoreTileSpec(
                            Icons.Default.DashboardCustomize, "Home panels",
                            null, onNavigateToHomePanels
                        ),
                    )
                )
            }
        }
    }
}

/** `null` (not loaded) renders blank; a count renders as "8 active" / "3" when unqualified. */
private fun Int?.countStatus(qualifier: String): String? = when {
    this == null -> null
    qualifier.isBlank() -> toString()
    else -> "$this $qualifier"
}

/** One tile's content, so a section can be declared as data and laid out on a real grid. */
private data class MoreTileSpec(
    val icon: ImageVector,
    val label: String,
    val status: String?,
    val onClick: () -> Unit,
)

/**
 * A titled 2-column grid of tiles.
 *
 * The rows are laid out here rather than emitted as siblings by the caller: doing the latter
 * stacked them with no gap at all, because spacing inside a `LazyColumn` `item` is the item's
 * own business. One [Column] with a single `spacedBy` keeps the vertical gutter equal to the
 * horizontal one, so the grid reads as a grid.
 */
@Composable
private fun MoreSection(
    title: String,
    tiles: List<MoreTileSpec>,
    topSpacing: androidx.compose.ui.unit.Dp = Spacing.sm,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeaderV2(
            title = title,
            topSpacing = topSpacing,
            modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
        )
        tiles.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                row.forEach { spec ->
                    MoreTile(
                        icon = spec.icon,
                        label = spec.label,
                        status = spec.status,
                        onClick = spec.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // An odd final tile stays half-width instead of stretching across.
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val COLUMNS = 2

/**
 * One hub tile: glyph, label, and the live status line that justifies the hub existing.
 *
 * Height is driven by content rather than fixed, so a wrapped two-line label ("Unrecognised
 * SMS") doesn't clip — but both tiles in a row stay equal height because the Row stretches
 * them, which is why the status line reserves its space even when absent.
 */
@Composable
private fun MoreTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.avatarLarge)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {}
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = label,
                    style = PennyWiseText.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // A blank line rather than nothing, so tiles with and without a status
                    // line up on a shared baseline instead of jostling as counts load.
                    text = status ?: " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
