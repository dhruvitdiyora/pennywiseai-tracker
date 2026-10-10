package com.pennywiseai.tracker.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.NotificationBing
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.ui.viewmodel.PermissionViewModel

/**
 * The standalone SMS-access screen. Same Cashiro-style layout as the onboarding
 * permission step (hero, grouped permission rows, privacy card, large action);
 * the permission requests and the notification-access hand-off are unchanged.
 */
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var readSmsGranted by remember { mutableStateOf(false) }
    var receiveSmsGranted by remember { mutableStateOf(false) }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
        receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true

        if (readSmsGranted) {
            viewModel.onPermissionResult(true)
            onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val notificationAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshNotificationAccess()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshNotificationAccess()
    }

    PennyWiseScaffold(
        modifier = modifier,
        customTopBar = {},
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
            ) {
                FirstRunPrimaryButton(
                    text = stringResource(R.string.permission_enable_button),
                    onClick = {
                        val permissions = mutableListOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        multiplePermissionLauncher.launch(permissions.toTypedArray())
                    }
                )
            }
        }
    ) { innerPadding ->
        FirstRunColumn(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            FirstRunHero {
                FirstRunHeroIcon(icon = Iconax.Messages)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            FirstRunHeading(
                title = stringResource(R.string.permission_title),
                body = stringResource(R.string.permission_description),
                titleStyle = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            FirstRunPermissionRows()

            Spacer(modifier = Modifier.height(Spacing.md))

            FirstRunInfoCard(
                title = stringResource(R.string.permission_privacy_title),
                body = stringResource(R.string.permission_privacy_points)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            NotificationAccessCard(
                hasNotificationAccess = uiState.hasNotificationAccess,
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    notificationAccessLauncher.launch(intent)
                }
            )

            if (uiState.showRationale) {
                Spacer(modifier = Modifier.height(Spacing.md))
                FirstRunBanner(
                    icon = Iconax.Danger,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    message = stringResource(R.string.permission_rationale)
                )
            }
        }
    }
}

/**
 * Bank-app notification access is a separate system setting from the SMS
 * permission, so it gets its own card: what it does, then either the state
 * ("enabled") or the button that opens the system screen to turn it on.
 */
@Composable
private fun NotificationAccessCard(
    hasNotificationAccess: Boolean,
    onOpenSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            IconTile(
                icon = Iconax.NotificationBing,
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer
            )
            RowLabels(
                title = stringResource(R.string.permission_notification_title),
                subtitle = stringResource(R.string.permission_notification_description),
                titleMaxLines = 2,
                subtitleMaxLines = Int.MAX_VALUE
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        if (hasNotificationAccess) {
            val incomeColor = scheme.income
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = incomeColor,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Text(
                    text = stringResource(R.string.permission_notification_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = incomeColor
                )
            }
        } else {
            FirstRunSecondaryButton(
                text = stringResource(R.string.permission_notification_open_settings),
                onClick = onOpenSettings
            )
        }
    }
}
