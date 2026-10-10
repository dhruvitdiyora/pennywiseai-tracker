package com.pennywiseai.tracker.ui.screens.rules

import com.pennywiseai.tracker.presentation.accounts.glassRowColor
import com.pennywiseai.tracker.presentation.accounts.glassRowRim
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.rules.RuleSharingCodec
import com.pennywiseai.tracker.domain.model.rule.TransactionRule
import com.pennywiseai.tracker.domain.usecase.BatchApplyResult
import com.pennywiseai.tracker.domain.usecase.DryRunResult
import com.pennywiseai.tracker.presentation.accounts.AccountMenuItem
import com.pennywiseai.tracker.presentation.people.PeopleExtendedFab
import com.pennywiseai.tracker.presentation.people.PeopleTonalActionButton
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Bag
import com.pennywiseai.tracker.ui.icons.iconax.Copy
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.ExportArrow01
import com.pennywiseai.tracker.ui.icons.iconax.Eye
import com.pennywiseai.tracker.ui.icons.iconax.History
import com.pennywiseai.tracker.ui.icons.iconax.ImportArrow01
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.icons.iconax.Magicpen
import com.pennywiseai.tracker.ui.icons.iconax.RefreshArrow01
import com.pennywiseai.tracker.ui.screens.settings.SettingsSection
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.RulesViewModel
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/*
 * Smart Rules, in the Cashiro style: a large collapsing title with tonal back and
 * overflow buttons, a primary hero card, rules grouped by purpose into connected
 * rows (name, description, switch and the per-rule menu), and a tonal extended
 * "create" button. Rule behaviour (toggle, edit, duplicate, apply to past,
 * delete, export/import/reset, the free-tier rule cap) is unchanged.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRule: () -> Unit,
    onNavigateToEditRule: (String) -> Unit,
    onNavigateToDuplicateRule: (String) -> Unit,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val batchApplyProgress by viewModel.batchApplyProgress.collectAsStateWithLifecycle()
    val batchApplyResult by viewModel.batchApplyResult.collectAsStateWithLifecycle()
    val dryRunResult by viewModel.dryRunResult.collectAsStateWithLifecycle()
    val canCreateMoreRules by viewModel.canCreateMoreRules.collectAsStateWithLifecycle()
    val sharingMessage by viewModel.sharingMessage.collectAsStateWithLifecycle()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Rule sharing (#741). CreateDocument/OpenDocument keep the file in the
    // user's own storage — nothing leaves the device unless they share it.
    val exportRulesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(RuleSharingCodec.MIME_TYPE)
    ) { uri -> uri?.let { viewModel.exportRules(it) } }
    val importRulesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importRules(it) } }

    var showBatchApplyDialog by remember { mutableStateOf(false) }
    var selectedRuleForBatch by remember { mutableStateOf<TransactionRule?>(null) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The create button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    var showResetDialog by remember { mutableStateOf(false) }

    val onCreateRule: () -> Unit = {
        if (canCreateMoreRules) onNavigateToCreateRule()
        else showUpgradeSheet = true
    }

    // Group rules by purpose for better organisation (same matching as before).
    val ruleGroups = remember(rules) {
        rules.groupBy { ruleGroupTitle(it) }.entries.toList()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.rules_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.rules_navigate_back),
                    )
                },
                actionContent = {
                    // The menu is anchored to the button; the screen gutter sits on
                    // the box so the menu lines up with the button itself.
                    Box(modifier = Modifier.padding(end = Dimensions.Padding.content)) {
                        PeopleTonalActionButton(
                            onClick = { showOverflowMenu = true },
                            icon = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.rules_more_options),
                            endPadding = Dimensions.Padding.none,
                        )
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            shape = MaterialTheme.shapes.large,
                        ) {
                            AccountMenuItem(
                                text = stringResource(R.string.rules_menu_export),
                                icon = Iconax.ExportArrow01,
                                onClick = {
                                    showOverflowMenu = false
                                    if (RuleSharingCodec.exportable(rules).isEmpty()) {
                                        viewModel.reportNothingToExport()
                                    } else {
                                        exportRulesLauncher.launch(
                                            "pennywise-rules.${RuleSharingCodec.FILE_EXTENSION}"
                                        )
                                    }
                                },
                            )
                            AccountMenuItem(
                                text = stringResource(R.string.rules_menu_import),
                                icon = Iconax.ImportArrow01,
                                onClick = {
                                    showOverflowMenu = false
                                    // Some file pickers don't offer JSON files under the
                                    // strict MIME type, so accept anything and let the
                                    // decoder reject what isn't a rule set.
                                    importRulesLauncher.launch(arrayOf("*/*"))
                                },
                            )
                            AccountMenuItem(
                                text = stringResource(R.string.rules_menu_reset),
                                icon = Iconax.RefreshArrow01,
                                onClick = {
                                    showOverflowMenu = false
                                    showResetDialog = true
                                },
                            )
                        }
                    }
                },
                hazeState = hazeState
            )
        },
        floatingActionButton = {
            PeopleExtendedFab(
                label = stringResource(R.string.rules_create_cd),
                onClick = onCreateRule,
                expanded = fabExpanded,
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() +
                        Dimensions.Component.fabScrollClearance
                ),
                state = lazyListState,
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap)
            ) {
                item(key = "hero") {
                    UtilityHeroCard(
                        icon = Iconax.Magicpen,
                        title = stringResource(R.string.rules_auto_categorization_title),
                        body = stringResource(R.string.rules_auto_categorization_body),
                    )
                }

                if (rules.isEmpty()) {
                    item(key = "empty") {
                        PennyWiseEmptyState(
                            icon = Iconax.Magicpen,
                            headline = stringResource(R.string.utility_rules_empty_title),
                            description = stringResource(R.string.utility_rules_empty_body),
                            actionLabel = stringResource(R.string.rules_create_cd),
                            onAction = onCreateRule,
                        )
                    }
                }

                ruleGroups.forEach { (titleRes, groupRules) ->
                    item(key = titleRes) {
                        SettingsSection(title = stringResource(titleRes)) {
                            GroupedList {
                                groupRules.forEachIndexed { index, rule ->
                                    RuleRow(
                                        rule = rule,
                                        position = ListItemPosition.from(index, groupRules.size),
                                        onToggle = { isActive ->
                                            viewModel.toggleRule(rule.id, isActive)
                                        },
                                        onEdit = {
                                            onNavigateToEditRule(rule.id)
                                        },
                                        onDuplicate = {
                                            onNavigateToDuplicateRule(rule.id)
                                        },
                                        onDelete = {
                                            viewModel.deleteRule(rule.id)
                                        },
                                        onApplyToPast = {
                                            selectedRuleForBatch = rule
                                            showBatchApplyDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Help text at the bottom
                item(key = "footer") {
                    Text(
                        text = stringResource(R.string.rules_footer_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.md)
                    )
                }
            }
        }
    }

    sharingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSharingMessage() },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Iconax.Information, contentDescription = null) },
            title = { Text(stringResource(R.string.rules_title)) },
            text = { Text(message.map { it.asString() }.joinToString(" ")) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSharingMessage() }) {
                    Text(stringResource(R.string.rules_ok))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Iconax.RefreshArrow01, contentDescription = null) },
            title = { Text(stringResource(R.string.rules_reset_title)) },
            text = { Text(stringResource(R.string.rules_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.rules_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.rules_cancel))
                }
            }
        )
    }

    // Batch Apply Dialog
    if (showBatchApplyDialog && selectedRuleForBatch != null) {
        BatchApplyDialog(
            rule = selectedRuleForBatch!!,
            progress = batchApplyProgress,
            result = batchApplyResult,
            dryRunResult = dryRunResult,
            isLoading = isLoading,
            onDismiss = {
                showBatchApplyDialog = false
                selectedRuleForBatch = null
                viewModel.clearBatchApplyResult()
            },
            onPreview = {
                viewModel.previewRule(selectedRuleForBatch!!)
            },
            onApplyToAll = {
                viewModel.applyRuleToPastTransactions(selectedRuleForBatch!!, applyToUncategorizedOnly = false)
            },
            onApplyToUncategorized = {
                viewModel.applyRuleToPastTransactions(selectedRuleForBatch!!, applyToUncategorizedOnly = true)
            }
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }
}

/**
 * The purpose group a rule is listed under. The built-in templates are grouped by
 * their (English, shipped) name; a custom rule is never matched on its name, since
 * a user-chosen "Rent split" is not the built-in rent rule, so it lands in "Custom".
 */
@StringRes
private fun ruleGroupTitle(rule: TransactionRule): Int {
    if (!rule.isSystemTemplate) return R.string.rules_group_custom
    return when {
        rule.name.contains("Food", ignoreCase = true) ||
            rule.name.contains("Fuel", ignoreCase = true) -> R.string.rules_group_daily

        rule.name.contains("Salary", ignoreCase = true) ||
            rule.name.contains("Cashback", ignoreCase = true) -> R.string.rules_group_income

        rule.name.contains("Rent", ignoreCase = true) ||
            rule.name.contains("EMI", ignoreCase = true) ||
            rule.name.contains("Subscription", ignoreCase = true) -> R.string.rules_group_recurring

        rule.name.contains("Investment", ignoreCase = true) ||
            rule.name.contains("Transfer", ignoreCase = true) -> R.string.rules_group_banking

        rule.name.contains("Healthcare", ignoreCase = true) -> R.string.rules_group_healthcare

        else -> R.string.rules_group_other
    }
}

/**
 * A short plain-language summary for the built-in rules, or null for anything else.
 * Only system templates are matched on their name; a custom rule never gets a
 * built-in description (see [ruleGroupTitle]) and is summarised from its own
 * conditions instead.
 */
@StringRes
private fun ruleConditionSummary(rule: TransactionRule): Int? {
    if (!rule.isSystemTemplate) return null
    return when {
        rule.name.contains("Small Payments", ignoreCase = true) -> R.string.rules_summary_small_payments
        rule.name.contains("UPI Cashback", ignoreCase = true) -> R.string.rules_summary_upi_cashback
        rule.name.contains("Salary", ignoreCase = true) -> R.string.rules_summary_salary
        rule.name.contains("Rent", ignoreCase = true) -> R.string.rules_summary_rent
        rule.name.contains("EMI", ignoreCase = true) -> R.string.rules_summary_emi
        rule.name.contains("Investment", ignoreCase = true) -> R.string.rules_summary_investment
        rule.name.contains("Subscription", ignoreCase = true) -> R.string.rules_summary_subscription
        rule.name.contains("Fuel", ignoreCase = true) -> R.string.rules_summary_fuel
        rule.name.contains("Healthcare", ignoreCase = true) -> R.string.rules_summary_healthcare
        rule.name.contains("Transfer", ignoreCase = true) -> R.string.rules_summary_transfer
        else -> null
    }
}

/** The info-icon line under a rule row's description. */
@Composable
private fun RuleSummaryLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Iconax.Information,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * One rule as a row of a connected group: its name, description, the plain-language
 * summary and a priority tag on the left; the per-rule menu (active rules only) and
 * the on/off switch on the right.
 */
@Composable
private fun RuleRow(
    rule: TransactionRule,
    position: ListItemPosition,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onApplyToPast: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

    GroupedRow(
        position = position,
        modifier = Modifier.glassRowRim(position),
        containerColor = glassRowColor(),
        minHeight = Dimensions.Component.listItemMinHeightTwoLine,
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.sm,
            top = Spacing.sm,
            bottom = Spacing.sm
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = rule.name,
                style = PennyWiseText.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            rule.description?.let { description ->
                Text(
                    text = description,
                    style = PennyWiseText.rowSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Built-in rules read their shipped summary; a custom rule without a
            // description of its own reads as "When <conditions>, <actions>",
            // built from what it actually does.
            val builtInSummary = ruleConditionSummary(rule)
            if (builtInSummary != null) {
                RuleSummaryLine(text = stringResource(builtInSummary))
            } else if (!rule.isSystemTemplate &&
                rule.description.isNullOrBlank() &&
                rule.conditions.isNotEmpty() &&
                rule.actions.isNotEmpty()
            ) {
                RuleSummaryLine(text = ruleSummarySentence(rule.conditions, rule.actions))
            }

            // Priority tag (only shown for a non-default priority)
            if (rule.priority != 100) {
                SubtitleTag(
                    text = stringResource(R.string.rules_priority_badge, rule.priority),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // More actions menu - only show when rule is active
        if (rule.isActive) {
            Box {
                IconButton(onClick = { showActionsMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.rules_more_actions),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false },
                    shape = MaterialTheme.shapes.large
                ) {
                    // Edit rule
                    AccountMenuItem(
                        text = stringResource(R.string.rules_menu_edit),
                        icon = Iconax.Edit2,
                        onClick = {
                            showActionsMenu = false
                            onEdit()
                        }
                    )

                    // Duplicate rule (opens the editor prefilled as a new rule)
                    AccountMenuItem(
                        text = stringResource(R.string.rules_menu_duplicate),
                        icon = Iconax.Copy,
                        onClick = {
                            showActionsMenu = false
                            onDuplicate()
                        }
                    )

                    // Apply to past transactions
                    AccountMenuItem(
                        text = stringResource(R.string.rules_menu_apply_past),
                        icon = Iconax.History,
                        onClick = {
                            showActionsMenu = false
                            onApplyToPast()
                        }
                    )

                    // Only show delete for custom rules
                    if (!rule.isSystemTemplate) {
                        AccountMenuItem(
                            text = stringResource(R.string.rules_delete_title),
                            icon = Iconax.Bag,
                            destructive = true,
                            onClick = {
                                showActionsMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        Switch(
            checked = rule.isActive,
            onCheckedChange = onToggle
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Icon(
                    Iconax.Bag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.rules_delete_title)) },
            text = { Text(stringResource(R.string.rules_delete_body, rule.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.rules_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.rules_cancel))
                }
            }
        )
    }
}

@Composable
private fun BatchApplyDialog(
    rule: TransactionRule,
    progress: Pair<Int, Int>?,
    result: BatchApplyResult?,
    dryRunResult: DryRunResult?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPreview: () -> Unit,
    onApplyToAll: () -> Unit,
    onApplyToUncategorized: () -> Unit
) {
    val title = when {
        progress != null -> stringResource(R.string.rules_batch_applying)
        isLoading && dryRunResult == null -> stringResource(R.string.rules_batch_previewing)
        dryRunResult != null && result == null -> stringResource(R.string.rules_batch_preview_title, rule.name)
        else -> stringResource(R.string.rules_batch_title)
    }

    AlertDialog(
        onDismissRequest = {
            if (progress == null && !isLoading) {
                onDismiss()
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Iconax.History, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                when {
                    // Loading preview
                    isLoading && dryRunResult == null && progress == null && result == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.rules_batch_scanning), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Preview result (dry run) — before actual apply
                    dryRunResult != null && result == null && progress == null -> {
                        if (dryRunResult.totalMatched == 0) {
                            Text(
                                text = stringResource(R.string.rules_batch_no_matches, dryRunResult.totalScanned),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = pluralStringResource(R.plurals.rules_batch_scanned, dryRunResult.totalScanned, dryRunResult.totalScanned),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.rules_batch_would_update, dryRunResult.totalWouldUpdate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            if (dryRunResult.totalWouldBlock > 0) {
                                Text(
                                    text = stringResource(R.string.rules_batch_would_block, dryRunResult.totalWouldBlock),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (dryRunResult.samples.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                                Text(
                                    text = stringResource(R.string.rules_batch_sample_changes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                dryRunResult.samples.take(5).forEach { diff ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (diff.isBlock)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(Spacing.smd),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                                        ) {
                                            val orig = diff.original
                                            Text(
                                                text = stringResource(
                                                    R.string.utility_rules_batch_sample_title,
                                                    orig.merchantName,
                                                    CurrencyFormatter.formatCurrency(orig.amount, orig.currency)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (diff.isBlock) {
                                                Text(
                                                    text = stringResource(R.string.rules_batch_sample_blocked),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            } else if (diff.modified != null) {
                                                val mod = diff.modified
                                                if (orig.category != mod.category) {
                                                    Text(
                                                        text = stringResource(R.string.rules_batch_change_category, orig.category, mod.category),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                if (orig.merchantName != mod.merchantName) {
                                                    Text(
                                                        text = stringResource(R.string.rules_batch_change_merchant, orig.merchantName, mod.merchantName),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                if (orig.transactionType != mod.transactionType) {
                                                    Text(
                                                        text = stringResource(R.string.rules_batch_change_type, orig.transactionType, mod.transactionType),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                if (orig.description != mod.description) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.rules_batch_change_description,
                                                            orig.description ?: stringResource(R.string.rules_batch_none),
                                                            mod.description ?: stringResource(R.string.rules_batch_none)
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                if (diff.tagChanges.isNotEmpty()) {
                                                    Text(
                                                        text = stringResource(R.string.rules_batch_change_tags, diff.tagChanges.joinToString(", ")),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (dryRunResult.totalMatched > 5) {
                                    Text(
                                        text = stringResource(R.string.rules_batch_and_more, dryRunResult.totalMatched - 5),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Initial state — show options with preview button
                    progress == null && result == null -> {
                        Text(
                            text = stringResource(R.string.rules_batch_confirm, rule.name),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.rules_batch_preview_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Processing state
                    progress != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = pluralStringResource(R.plurals.rules_batch_processing, progress.second, progress.first, progress.second),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Result state
                    result != null -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (result.errors.isEmpty()) Icons.Default.CheckCircle else Iconax.Danger,
                                    contentDescription = null,
                                    tint = if (result.errors.isEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = stringResource(R.string.rules_batch_completed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))

                            Text(
                                text = stringResource(R.string.rules_batch_result_processed, result.totalProcessed),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(R.string.rules_batch_result_updated, result.totalUpdated),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            if (result.totalDeleted > 0) {
                                Text(
                                    text = stringResource(R.string.rules_batch_result_blocked, result.totalDeleted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (result.errors.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.rules_batch_result_errors, result.errors.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                // Loading — no buttons
                isLoading -> {}

                // Preview result — show Apply or Cancel
                dryRunResult != null && result == null && progress == null -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.rules_cancel)) }
                        if (dryRunResult.totalMatched > 0) {
                            TextButton(onClick = onApplyToUncategorized) { Text(stringResource(R.string.rules_batch_apply_uncategorized)) }
                            TextButton(onClick = onApplyToAll) { Text(stringResource(R.string.rules_batch_apply_all)) }
                        }
                    }
                }

                // Initial state — show Preview + direct apply buttons
                progress == null && result == null -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.rules_cancel)) }
                        FilledTonalButton(onClick = onPreview) {
                            Icon(Iconax.Eye, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.small))
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(stringResource(R.string.rules_batch_preview))
                        }
                    }
                }

                // Done — close button
                result != null -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.rules_close)) }
                }
            }
        }
    )
}
