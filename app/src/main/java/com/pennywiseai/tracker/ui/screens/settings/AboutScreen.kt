package com.pennywiseai.tracker.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.BuildConfig
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.icons.iconax.CodeCircle
import com.pennywiseai.tracker.ui.icons.iconax.ExportArrow02
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.SecuritySafe
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AboutScreenContent(
        versionName = BuildConfig.VERSION_NAME,
        onNavigateBack = onNavigateBack,
        onOpenSourceCode = {
            openExternalLink(
                context = context,
                url = Constants.Links.GITHUB_URL,
                errorMessage = context.getString(R.string.about_open_link_error),
            )
        },
        onNavigateToLicenses = onNavigateToLicenses,
        modifier = modifier,
    )
}

/**
 * About, in Cashiro's layout: an identity header (round icon, name, version)
 * followed by grouped link rows with tonal icon tiles. Only PennyWise's own
 * identity, source link and credits are shown; Cashiro's developer card,
 * website, community and legal links are not carried over.
 */
@Composable
internal fun AboutScreenContent(
    versionName: String,
    onNavigateBack: () -> Unit,
    onOpenSourceCode: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubScreen(
        title = stringResource(R.string.about_title),
        backContentDescription = stringResource(R.string.about_back),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        AboutHeader(versionName = versionName)

        SettingsSection(title = stringResource(R.string.about_privacy_section)) {
            PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(
                            icon = Iconax.SecuritySafe,
                            containerColor = green_light,
                            contentColor = green_dark,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text(
                                text = stringResource(R.string.about_privacy_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.about_privacy_summary),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.about_privacy_exceptions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.about_project_section)) {
            GroupedList {
                SettingsIconRow(
                    icon = Iconax.CodeCircle,
                    iconContainerColor = green_light,
                    iconContentColor = green_dark,
                    title = stringResource(R.string.about_source_code),
                    subtitle = stringResource(R.string.about_source_code_subtitle),
                    trailingIcon = Iconax.ExportArrow02,
                    position = ListItemPosition.Top,
                    onClick = onOpenSourceCode,
                )
                SettingsIconRow(
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    iconContainerColor = purple_light,
                    iconContentColor = purple_dark,
                    title = stringResource(R.string.about_selected_dependencies),
                    subtitle = stringResource(R.string.about_selected_dependencies_subtitle),
                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    position = ListItemPosition.Bottom,
                    onClick = onNavigateToLicenses,
                )
            }
        }
    }
}

@Composable
private fun AboutHeader(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.Padding.card),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        // PennyWise's own launcher mark on a tonal disc. The name below
        // already labels it, so the image itself is decorative.
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.extraLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.about_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_open_source_license),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

internal data class DependencyNotice(
    val name: String,
    val organization: String,
    val purpose: String,
)

@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedNotice by remember { mutableStateOf<DependencyNotice?>(null) }

    LicensesScreenContent(
        onNavigateBack = onNavigateBack,
        onDependencyClick = { selectedNotice = it },
        modifier = modifier,
    )

    selectedNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { selectedNotice = null },
            title = { Text(notice.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = notice.organization,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = notice.purpose,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.licenses_apache_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNotice = null }) {
                    Text(stringResource(R.string.licenses_done))
                }
            },
        )
    }
}

@Composable
internal fun LicensesScreenContent(
    onNavigateBack: () -> Unit,
    onDependencyClick: (DependencyNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notices = selectedDependencyNotices()

    SettingsSubScreen(
        title = stringResource(R.string.licenses_title),
        backContentDescription = stringResource(R.string.licenses_back),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GroupedList {
            notices.forEachIndexed { index, notice ->
                SettingsIconRow(
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    iconContainerColor = orange_light,
                    iconContentColor = orange_dark,
                    title = notice.name,
                    subtitle = notice.purpose,
                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    position = ListItemPosition.from(index, notices.size),
                    onClick = { onDependencyClick(notice) },
                )
            }
        }
    }
}

@Composable
private fun selectedDependencyNotices(): List<DependencyNotice> = listOf(
    DependencyNotice(
        name = stringResource(R.string.licenses_androidx_name),
        organization = stringResource(R.string.licenses_google),
        purpose = stringResource(R.string.licenses_androidx_purpose),
    ),
    DependencyNotice(
        name = stringResource(R.string.licenses_compose_name),
        organization = stringResource(R.string.licenses_google),
        purpose = stringResource(R.string.licenses_compose_purpose),
    ),
    DependencyNotice(
        name = stringResource(R.string.licenses_room_name),
        organization = stringResource(R.string.licenses_google),
        purpose = stringResource(R.string.licenses_room_purpose),
    ),
    DependencyNotice(
        name = stringResource(R.string.licenses_hilt_name),
        organization = stringResource(R.string.licenses_google),
        purpose = stringResource(R.string.licenses_hilt_purpose),
    ),
    DependencyNotice(
        name = stringResource(R.string.licenses_kotlin_name),
        organization = stringResource(R.string.licenses_jetbrains),
        purpose = stringResource(R.string.licenses_kotlin_purpose),
    ),
)

internal fun openExternalLink(
    context: Context,
    url: String,
    errorMessage: String,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }
}
