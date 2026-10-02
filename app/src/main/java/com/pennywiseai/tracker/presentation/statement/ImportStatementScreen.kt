package com.pennywiseai.tracker.presentation.statement

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.statement.StatementImportResult
import com.pennywiseai.tracker.ui.components.SupportDevelopmentDialog
import com.pennywiseai.tracker.ui.components.SupportNudgeCard
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.icons.iconax.DocumentText2
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.ImportArrow01
import com.pennywiseai.tracker.ui.icons.iconax.RefreshArrow01
import com.pennywiseai.tracker.ui.screens.rules.UtilityHeroCard
import com.pennywiseai.tracker.ui.screens.settings.SettingsSubScreen
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Import Statement, in the Cashiro style (there is no Cashiro counterpart, so it
 * follows the other utility screens): a large collapsing title with a tonal back
 * button, then one calm block per state: a hero card and a primary action when
 * idle, a tonal progress card while importing, a status badge with the result
 * counts as a connected group when done, and a retry when it failed. The import
 * flow and the monthly free-tier gate are unchanged.
 */

@Composable
fun ImportStatementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportStatementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canImportThisMonth by viewModel.canImportThisMonth.collectAsStateWithLifecycle()
    val showSupportNudge by viewModel.showSupportNudge.collectAsStateWithLifecycle()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.importStatement(it) }
        }
    )

    // Free users get 1 import / calendar month; Pro is unlimited. Centralising
    // the gate here means all three "select PDF" entry points (Idle / Success /
    // Error) share the same enforcement without sprinkling checks.
    val onTryLaunchPicker: () -> Unit = {
        if (canImportThisMonth) pdfPicker.launch("application/pdf")
        else showUpgradeSheet = true
    }

    SettingsSubScreen(
        title = stringResource(R.string.import_statement_title),
        backContentDescription = stringResource(R.string.import_statement_back),
        onNavigateBack = onNavigateBack,
    ) {
        when (val state = uiState) {
            is ImportStatementUiState.Idle -> IdleContent(
                onSelectPdf = onTryLaunchPicker
            )
            is ImportStatementUiState.Loading -> LoadingContent()
            is ImportStatementUiState.Success -> SuccessContent(
                result = state.result,
                showSupportNudge = showSupportNudge,
                onSupportClick = {
                    showSupportDialog = true
                    viewModel.dismissSupportNudge()
                },
                onImportAnother = {
                    viewModel.resetState()
                    onTryLaunchPicker()
                },
                onDone = onNavigateBack
            )
            is ImportStatementUiState.Error -> ErrorContent(
                message = state.message,
                onTryAgain = {
                    viewModel.resetState()
                    onTryLaunchPicker()
                }
            )
        }
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }

    if (showSupportDialog) {
        SupportDevelopmentDialog(onDismiss = { showSupportDialog = false })
    }
}

/** The primary full-width action of the screen. */
@Composable
private fun StatementActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false
) {
    val content: @Composable () -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.medium)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
    if (tonal) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(Dimensions.Component.listItemMinHeight)
        ) { content() }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(Dimensions.Component.listItemMinHeight)
        ) { content() }
    }
}

/** A round tonal badge that states how the import ended (done, failed). */
@Composable
private fun StatusBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .size(Dimensions.Icon.emptyStateContainer)
            .background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.emptyStateGlyph),
            tint = contentColor
        )
    }
}

@Composable
private fun IdleContent(onSelectPdf: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        UtilityHeroCard(
            icon = Iconax.DocumentText2,
            title = stringResource(R.string.import_statement_title),
            body = stringResource(R.string.import_statement_description)
        )

        StatementActionButton(
            text = stringResource(R.string.import_statement_select_pdf),
            icon = Iconax.ImportArrow01,
            onClick = onSelectPdf
        )
    }
}

@Composable
private fun LoadingContent() {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = Spacing.lg
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.Icon.avatarLarge),
                strokeWidth = Dimensions.Component.progressRingStroke
            )

            Text(
                text = stringResource(R.string.import_statement_loading_title),
                modifier = Modifier.padding(top = Spacing.sm),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.import_statement_loading_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** One label / count line of the result summary. */
private data class ResultLine(
    val label: String,
    val value: String,
    val isHighlighted: Boolean = false
)

@Composable
private fun SuccessContent(
    result: StatementImportResult.Success,
    showSupportNudge: Boolean = false,
    onSupportClick: () -> Unit = {},
    onImportAnother: () -> Unit,
    onDone: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val summaryLines = listOfNotNull(
        ResultLine(
            label = stringResource(R.string.import_statement_result_imported),
            value = "${result.imported}",
            isHighlighted = true
        ),
        if (result.enriched > 0) {
            ResultLine(
                label = stringResource(R.string.import_statement_result_enriched),
                value = "${result.enriched}",
                isHighlighted = true
            )
        } else {
            null
        },
        ResultLine(
            label = stringResource(R.string.import_statement_result_total_parsed),
            value = "${result.totalParsed}"
        )
    )
    val duplicateLines = listOfNotNull(
        if (result.skippedByHash > 0) {
            ResultLine(
                label = stringResource(R.string.import_statement_result_exact),
                value = "${result.skippedByHash}"
            )
        } else {
            null
        },
        if (result.skippedByReference > 0) {
            ResultLine(
                label = stringResource(R.string.import_statement_result_by_reference),
                value = "${result.skippedByReference}"
            )
        } else {
            null
        },
        if (result.skippedByAmountDate > 0) {
            ResultLine(
                label = stringResource(R.string.import_statement_result_by_amount_date),
                value = "${result.skippedByAmountDate}"
            )
        } else {
            null
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        StatusBadge(
            icon = Icons.Default.CheckCircle,
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer
        )

        Text(
            text = stringResource(R.string.import_statement_complete),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            textAlign = TextAlign.Center
        )

        // Results, as one connected group
        GroupedList {
            summaryLines.forEachIndexed { index, line ->
                ResultRow(
                    line = line,
                    position = ListItemPosition.from(index, summaryLines.size)
                )
            }
        }

        if (result.skippedDuplicates > 0) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)
            ) {
                SectionHeaderV2(
                    title = stringResource(
                        R.string.import_statement_result_duplicates,
                        result.skippedDuplicates
                    ),
                    topSpacing = Spacing.none
                )
                if (duplicateLines.isNotEmpty()) {
                    GroupedList {
                        duplicateLines.forEachIndexed { index, line ->
                            ResultRow(
                                line = line,
                                position = ListItemPosition.from(index, duplicateLines.size)
                            )
                        }
                    }
                }
            }
        }

        if (showSupportNudge) {
            SupportNudgeCard(onClick = onSupportClick)
        }

        StatementActionButton(
            text = stringResource(R.string.import_statement_import_another),
            icon = Iconax.ImportArrow01,
            onClick = onImportAnother,
            modifier = Modifier.padding(top = Spacing.sm)
        )

        StatementActionButton(
            text = stringResource(R.string.import_statement_done),
            icon = Icons.Default.CheckCircle,
            onClick = onDone,
            tonal = true
        )
    }
}

@Composable
private fun ResultRow(
    line: ResultLine,
    position: ListItemPosition
) {
    GroupedRow(
        position = position,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = line.label,
            modifier = Modifier.weight(1f),
            style = if (line.isHighlighted) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = if (line.isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (line.isHighlighted) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = line.value,
            style = if (line.isHighlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (line.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (line.isHighlighted) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onTryAgain: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        StatusBadge(
            icon = Iconax.Danger,
            containerColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer
        )

        Text(
            text = stringResource(R.string.import_statement_failed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        StatementActionButton(
            text = stringResource(R.string.import_statement_try_again),
            icon = Iconax.RefreshArrow01,
            onClick = onTryAgain,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}
