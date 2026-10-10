package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.icons.iconax.Folder2
import com.pennywiseai.tracker.ui.icons.iconax.ExportArrow01
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.ImportArrow01
import com.pennywiseai.tracker.ui.icons.iconax.Magicpen
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.NotificationBing
import com.pennywiseai.tracker.ui.icons.iconax.SecuritySafe
import com.pennywiseai.tracker.ui.icons.iconax.Send
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.amber_dark
import com.pennywiseai.tracker.ui.theme.amber_light
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.teal_light

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

/**
 * Data & privacy, in the Cashiro settings style: a tonal hero card followed by
 * grouped rows with colour-coded icon tiles, under a large collapsing title.
 *
 * PennyWise's page is an explanation of what stays local and when data can
 * leave the device, so its rows are informational. Cashiro's version of this
 * page hosts the app-lock switch and timeout; PennyWise keeps App Lock in
 * Settings, so no actions move here.
 */
@Composable
internal fun DataPrivacyScreenContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubScreen(
        title = stringResource(R.string.data_privacy_title),
        backContentDescription = stringResource(R.string.data_privacy_back),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        PrivacyHero()

        PrivacyGroup(
            title = stringResource(R.string.data_privacy_local_section),
            rows = listOf(
                PrivacyRowModel(
                    icon = Iconax.Folder2,
                    containerColor = blue_light,
                    contentColor = blue_dark,
                    title = stringResource(R.string.data_privacy_records_title),
                    body = stringResource(R.string.data_privacy_records_body),
                ),
                PrivacyRowModel(
                    icon = Iconax.Messages,
                    containerColor = green_light,
                    contentColor = green_dark,
                    title = stringResource(R.string.data_privacy_messages_title),
                    body = stringResource(R.string.data_privacy_messages_body),
                ),
                PrivacyRowModel(
                    icon = Iconax.Magicpen,
                    containerColor = purple_light,
                    contentColor = purple_dark,
                    title = stringResource(R.string.data_privacy_ai_title),
                    body = stringResource(R.string.data_privacy_ai_body),
                ),
            ),
        )

        PrivacyGroup(
            title = stringResource(R.string.data_privacy_access_section),
            rows = listOf(
                PrivacyRowModel(
                    icon = Iconax.NotificationBing,
                    containerColor = orange_light,
                    contentColor = orange_dark,
                    title = stringResource(R.string.data_privacy_sms_access_title),
                    body = stringResource(R.string.data_privacy_sms_access_body),
                ),
                PrivacyRowModel(
                    icon = Icons.Default.Contacts,
                    containerColor = teal_light,
                    contentColor = teal_dark,
                    title = stringResource(R.string.data_privacy_contacts_access_title),
                    body = stringResource(R.string.data_privacy_contacts_access_body),
                ),
            ),
        )

        PrivacyGroup(
            title = stringResource(R.string.data_privacy_leaves_section),
            rows = listOf(
                PrivacyRowModel(
                    icon = Iconax.Send,
                    containerColor = red_light,
                    contentColor = red_dark,
                    title = stringResource(R.string.data_privacy_reports_title),
                    body = stringResource(R.string.data_privacy_reports_body),
                ),
                PrivacyRowModel(
                    icon = Iconax.ExportArrow01,
                    containerColor = amber_light,
                    contentColor = amber_dark,
                    title = stringResource(R.string.data_privacy_exports_title),
                    body = stringResource(R.string.data_privacy_exports_body),
                ),
                PrivacyRowModel(
                    icon = Iconax.ImportArrow01,
                    containerColor = cyan_light,
                    contentColor = cyan_dark,
                    title = stringResource(R.string.data_privacy_downloads_title),
                    body = stringResource(R.string.data_privacy_downloads_body),
                ),
            ),
        )
    }
}

@Composable
private fun PrivacyHero() {
    // The same extra-large primary card as the profile header on Settings.
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tint = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Iconax.SecuritySafe,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
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
    SettingsSection(title = title) {
        GroupedList {
            rows.forEachIndexed { index, row ->
                SettingsIconRow(
                    icon = row.icon,
                    iconContainerColor = row.containerColor,
                    iconContentColor = row.contentColor,
                    title = row.title,
                    subtitle = row.body,
                    subtitleMaxLines = 6,
                    position = ListItemPosition.from(index, rows.size),
                    minHeight = Dimensions.Component.listItemMinHeightTwoLine,
                )
            }
        }
    }
}

private data class PrivacyRowModel(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val title: String,
    val body: String,
)
