package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

@Composable
fun DataPrivacyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DataPrivacyScreenContent(
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
internal fun DataPrivacyScreenContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PennyWiseScaffold(
        modifier = modifier,
        title = stringResource(R.string.data_privacy_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.data_privacy_back),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = paddingValues.calculateTopPadding() + Spacing.md,
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
        ) {
            item { PrivacyHero() }

            item {
                PrivacyGroup(
                    title = stringResource(R.string.data_privacy_local_section),
                    rows = listOf(
                        PrivacyRowModel(
                            icon = Icons.Default.Storage,
                            title = stringResource(R.string.data_privacy_records_title),
                            body = stringResource(R.string.data_privacy_records_body),
                        ),
                        PrivacyRowModel(
                            icon = Icons.Default.Sms,
                            title = stringResource(R.string.data_privacy_messages_title),
                            body = stringResource(R.string.data_privacy_messages_body),
                        ),
                        PrivacyRowModel(
                            icon = Icons.Default.Memory,
                            title = stringResource(R.string.data_privacy_ai_title),
                            body = stringResource(R.string.data_privacy_ai_body),
                        ),
                    ),
                )
            }

            item {
                PrivacyGroup(
                    title = stringResource(R.string.data_privacy_access_section),
                    rows = listOf(
                        PrivacyRowModel(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.data_privacy_sms_access_title),
                            body = stringResource(R.string.data_privacy_sms_access_body),
                        ),
                        PrivacyRowModel(
                            icon = Icons.Default.Contacts,
                            title = stringResource(R.string.data_privacy_contacts_access_title),
                            body = stringResource(R.string.data_privacy_contacts_access_body),
                        ),
                    ),
                )
            }

            item {
                PrivacyGroup(
                    title = stringResource(R.string.data_privacy_leaves_section),
                    rows = listOf(
                        PrivacyRowModel(
                            icon = Icons.Default.PrivacyTip,
                            title = stringResource(R.string.data_privacy_reports_title),
                            body = stringResource(R.string.data_privacy_reports_body),
                        ),
                        PrivacyRowModel(
                            icon = Icons.Default.Share,
                            title = stringResource(R.string.data_privacy_exports_title),
                            body = stringResource(R.string.data_privacy_exports_body),
                        ),
                        PrivacyRowModel(
                            icon = Icons.Default.CloudDownload,
                            title = stringResource(R.string.data_privacy_downloads_title),
                            body = stringResource(R.string.data_privacy_downloads_body),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun PrivacyHero() {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Default.PrivacyTip,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = stringResource(R.string.data_privacy_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.data_privacy_hero_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PrivacyGroup(title: String, rows: List<PrivacyRowModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
        SectionHeaderV2(title = title, topSpacing = Spacing.none)
        GroupedList {
            rows.forEachIndexed { index, row ->
                GroupedRow(
                    position = ListItemPosition.from(index, rows.size),
                    minHeight = Dimensions.Component.listItemMinHeightTwoLine,
                ) {
                    IconTile(
                        icon = row.icon,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    RowLabels(
                        title = row.title,
                        subtitle = row.body,
                        subtitleMaxLines = 6,
                    )
                }
            }
        }
    }
}

private data class PrivacyRowModel(
    val icon: ImageVector,
    val title: String,
    val body: String,
)
