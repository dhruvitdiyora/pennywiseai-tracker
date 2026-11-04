package com.pennywiseai.tracker.ui.screens

import android.widget.Toast
import com.pennywiseai.tracker.ui.screens.settings.GlassGroupedRow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Padlock
import com.pennywiseai.tracker.ui.icons.iconax.SecuritySafe
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.AppLockViewModel

/**
 * The blocking lock screen. Cashiro-style layout over the existing behaviour:
 * a padlock hero, a bold heading, a large Unlock action (or, where the device
 * can't authenticate, an explanatory notice), and a tonal row stating how the
 * data is protected. Authentication, back-blocking and navigation are unchanged.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Prevent back navigation when app is locked
    BackHandler(enabled = true) {
        // Do nothing - prevent back navigation
        // User must authenticate to proceed
    }

    // Auto-trigger authentication when screen is shown
    // (With no screen lock at all, authenticate lets the user straight in.)
    LaunchedEffect(Unit) {
        if (context is FragmentActivity) {
            triggerAuthentication(context, viewModel)
        }
    }

    // Navigate away only on explicit authentication success
    LaunchedEffect(uiState.authenticationSucceeded) {
        if (uiState.authenticationSucceeded) {
            viewModel.resetAuthenticationSucceeded()
            onUnlocked()
        }
    }

    PennyWiseScaffold(
        modifier = modifier,
        customTopBar = {}
    ) { innerPadding ->
        FirstRunColumn(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.Center
        ) {
            // Lock icon
            FirstRunHero {
                FirstRunHeroIcon(
                    icon = Iconax.Padlock,
                    contentDescription = stringResource(R.string.applock_icon_cd)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Title and description
            FirstRunHeading(
                title = stringResource(R.string.applock_title),
                body = stringResource(R.string.applock_subtitle),
                titleStyle = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Show error if authentication failed
            val authenticationError = uiState.authenticationError
            if (authenticationError != null) {
                FirstRunBanner(
                    icon = Iconax.Danger,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    message = authenticationError.asString()
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Always offered: the prompt falls back to the device PIN, and a
            // device with no screen lock is let straight in.
            FirstRunPrimaryButton(
                text = stringResource(R.string.applock_unlock),
                onClick = {
                    if (context is FragmentActivity) {
                        viewModel.clearAuthError()
                        triggerAuthentication(context, viewModel)
                    }
                },
                trailingIcon = null
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Privacy note
            GlassGroupedRow(position = ListItemPosition.Single) {
                IconTile(
                    icon = Iconax.SecuritySafe,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = when (uiState.timeoutMinutes) {
                        0 -> stringResource(R.string.applock_protected_immediate)
                        else -> pluralStringResource(
                            R.plurals.applock_protected_timeout,
                            uiState.timeoutMinutes,
                            uiState.timeoutMinutes
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun triggerAuthentication(
    activity: FragmentActivity,
    viewModel: AppLockViewModel
) {
    // Application context: the notice can land after the lock screen has gone.
    val appContext = activity.applicationContext
    viewModel.triggerAuthentication(activity) {
        Toast.makeText(appContext, R.string.applock_no_screen_lock_notice, Toast.LENGTH_LONG).show()
    }
}
