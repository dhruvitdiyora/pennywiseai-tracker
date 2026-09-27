package com.pennywiseai.tracker.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.BuildConfig
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreenContent(
    versionName: String,
    onNavigateBack: () -> Unit,
    onOpenSourceCode: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    PennyWiseScaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        customTopBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehavior,
                scrollBehaviorLarge = scrollBehavior,
                title = stringResource(R.string.about_title),
                hasBackButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Spacing.md,
                bottom = Spacing.Layout.scrollBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
        ) {
            item {
                AboutHero(versionName = versionName)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(
                        title = stringResource(R.string.about_privacy_section),
                        topSpacing = Spacing.none,
                    )
                    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconTile(
                                    icon = Icons.Default.Security,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    SectionHeaderV2(
                        title = stringResource(R.string.about_project_section),
                        topSpacing = Spacing.none,
                    )
                    GroupedList {
                        AboutNavigationRow(
                            title = stringResource(R.string.about_source_code),
                            subtitle = stringResource(R.string.about_source_code_subtitle),
                            icon = Icons.Default.Code,
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            position = ListItemPosition.Top,
                            onClick = onOpenSourceCode,
                        )
                        AboutNavigationRow(
                            title = stringResource(R.string.about_selected_dependencies),
                            subtitle = stringResource(R.string.about_selected_dependencies_subtitle),
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            trailingIcon = Icons.Default.ChevronRight,
                            position = ListItemPosition.Bottom,
                            onClick = onNavigateToLicenses,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHero(versionName: String) {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = Spacing.lg,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(Dimensions.Icon.extraLarge),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.about_open_source_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutNavigationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    position: ListItemPosition,
    onClick: () -> Unit,
) {
    GroupedRow(position = position, onClick = onClick) {
        IconTile(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        RowLabels(title = title, subtitle = subtitle)
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.inline),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesScreenContent(
    onNavigateBack: () -> Unit,
    onDependencyClick: (DependencyNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val notices = selectedDependencyNotices()

    PennyWiseScaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        customTopBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehavior,
                scrollBehaviorLarge = scrollBehavior,
                title = stringResource(R.string.licenses_title),
                hasBackButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.licenses_back),
                        )
                    }
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Spacing.md,
                bottom = Spacing.Layout.scrollBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
        ) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                GroupedList {
                    notices.forEachIndexed { index, notice ->
                        GroupedRow(
                            position = ListItemPosition.from(index, notices.size),
                            onClick = { onDependencyClick(notice) },
                        ) {
                            IconTile(
                                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            RowLabels(
                                title = notice.name,
                                subtitle = notice.purpose,
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimensions.Icon.inline),
                            )
                        }
                    }
                }
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

private fun openExternalLink(
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
