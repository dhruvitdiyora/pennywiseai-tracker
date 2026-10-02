package com.pennywiseai.tracker.ui.screens.onboarding

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.screens.FirstRunPrimaryButton
import com.pennywiseai.tracker.ui.screens.FirstRunSecondaryButton
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Presentation-only onboarding shell backed by the existing ViewModel contract.
 *
 * Cashiro-style layout: a step progress bar across the top, the step's content
 * in the middle, and a pinned bottom bar with a large primary action (and a
 * tonal Skip under it where the step allows skipping). Every step, skip path
 * and callback is the one the ViewModel already defines; only the look moved.
 */
@Composable
fun OnboardingRefreshLayout(
    uiState: OnBoardingUiState,
    viewModel: OnBoardingViewModel,
    onComplete: () -> Unit
) {
    val stepOrder = remember { OnBoardingStep.entries.toList() }
    val reducedMotion = rememberReducedMotion()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onSmsPermissionResult(result[Manifest.permission.READ_SMS] == true)
    }

    PennyWiseScaffold(
        customTopBar = {
            OnboardingProgressBar(current = uiState.currentStep, reducedMotion = reducedMotion)
        },
        bottomBar = {
            OnboardingBottomBar(
                uiState = uiState,
                onBack = viewModel::goToPreviousStep,
                onNext = {
                    when (uiState.currentStep) {
                        OnBoardingStep.WELCOME,
                        OnBoardingStep.PROFILE,
                        OnBoardingStep.PERMISSIONS,
                        OnBoardingStep.SMS_SCAN -> viewModel.goToNextStep()
                        OnBoardingStep.ACCOUNT_SETUP -> viewModel.completeOnboarding(onComplete)
                    }
                },
                onSkip = {
                    when (uiState.currentStep) {
                        OnBoardingStep.PERMISSIONS -> {
                            viewModel.skipSmsPermission()
                            viewModel.goToNextStep()
                        }
                        OnBoardingStep.SMS_SCAN -> viewModel.goToNextStep()
                        OnBoardingStep.ACCOUNT_SETUP -> viewModel.completeOnboarding(onComplete)
                        else -> viewModel.goToNextStep()
                    }
                },
                onStartScan = viewModel::startSmsScan,
                onRequestPermissions = { permissionLauncher.launch(requestedPermissions()) }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = uiState.currentStep,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val targetIndex = stepOrder.indexOf(targetState)
                    val initialIndex = stepOrder.indexOf(initialState)
                    if (targetIndex > initialIndex) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                }
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                OnBoardingStep.WELCOME -> OnboardingWelcomeStep()
                OnBoardingStep.PROFILE -> OnboardingProfileStep(uiState, viewModel)
                OnBoardingStep.PERMISSIONS -> OnboardingPermissionsStep(uiState.smsPermissionGranted)
                OnBoardingStep.SMS_SCAN -> OnboardingScanStep(uiState)
                OnBoardingStep.ACCOUNT_SETUP -> OnboardingAccountStep(uiState, viewModel::selectAccount)
            }
        }
    }
}

/** The runtime permissions the onboarding permission step asks for. */
private fun requestedPermissions(): Array<String> {
    val permissions = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return permissions.toTypedArray()
}

/**
 * Progress across the five steps, drawn as one thick rounded bar at the top.
 * The fill glides to the new step (it snaps when the user has animations
 * turned off), and the bar reads as "Step N of 5" to accessibility services.
 */
@Composable
private fun OnboardingProgressBar(
    current: OnBoardingStep,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val total = OnBoardingStep.entries.size
    val position = OnBoardingStep.entries.indexOf(current) + 1
    val description = stringResource(R.string.onboarding_step_progress, position, total)
    val progress by animateFloatAsState(
        targetValue = position.toFloat() / total,
        animationSpec = if (reducedMotion) {
            snap<Float>()
        } else {
            spring<Float>(stiffness = Spring.StiffnessMediumLow)
        },
        label = "onboarding_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.md)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.Component.progressBarHeight)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/** What the primary button of the current step says and does. */
private class PrimaryActionSpec(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
)

@Composable
private fun primaryActionFor(
    uiState: OnBoardingUiState,
    onNext: () -> Unit,
    onStartScan: () -> Unit,
    onRequestPermissions: () -> Unit,
): PrimaryActionSpec? = when (uiState.currentStep) {
    OnBoardingStep.WELCOME -> PrimaryActionSpec(
        label = stringResource(R.string.onboarding_get_started),
        onClick = onNext
    )
    OnBoardingStep.PROFILE -> PrimaryActionSpec(
        label = stringResource(R.string.onboarding_save_continue),
        onClick = onNext,
        enabled = uiState.userName.isNotBlank()
    )
    OnBoardingStep.PERMISSIONS -> if (uiState.smsPermissionGranted) {
        PrimaryActionSpec(
            label = stringResource(R.string.onboarding_continue),
            onClick = onNext
        )
    } else {
        PrimaryActionSpec(
            label = stringResource(R.string.onboarding_permissions_enable),
            onClick = onRequestPermissions
        )
    }
    OnBoardingStep.SMS_SCAN -> when {
        uiState.isScanning -> null
        uiState.scanCompleted -> PrimaryActionSpec(
            label = stringResource(R.string.onboarding_continue),
            onClick = onNext
        )
        else -> PrimaryActionSpec(
            label = stringResource(R.string.onboarding_start_scanning),
            onClick = onStartScan
        )
    }
    OnBoardingStep.ACCOUNT_SETUP -> PrimaryActionSpec(
        label = stringResource(R.string.onboarding_finish),
        onClick = onNext,
        enabled = uiState.selectedAccountKey != null || uiState.accounts.isEmpty(),
        loading = uiState.isCompleting,
        icon = Icons.Default.Check
    )
}

/** Whether the current step offers a Skip under its primary action. */
private fun showsSkip(uiState: OnBoardingUiState): Boolean = when (uiState.currentStep) {
    OnBoardingStep.WELCOME,
    OnBoardingStep.PROFILE -> false
    OnBoardingStep.PERMISSIONS -> !uiState.smsPermissionGranted
    OnBoardingStep.SMS_SCAN -> uiState.isScanning || !uiState.scanCompleted
    OnBoardingStep.ACCOUNT_SETUP -> uiState.accounts.isNotEmpty() && uiState.selectedAccountKey == null
}

@Composable
private fun OnboardingBottomBar(
    uiState: OnBoardingUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStartScan: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val canGoBack = uiState.currentStep != OnBoardingStep.WELCOME && !uiState.isScanning
    val primary = primaryActionFor(uiState, onNext, onStartScan, onRequestPermissions)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (primary != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
            ) {
                if (canGoBack) {
                    OnboardingBackButton(onClick = onBack)
                }
                FirstRunPrimaryButton(
                    text = primary.label,
                    onClick = primary.onClick,
                    modifier = Modifier.weight(1f),
                    enabled = primary.enabled,
                    loading = primary.loading,
                    trailingIcon = primary.icon
                )
            }
        }
        if (showsSkip(uiState)) {
            FirstRunSecondaryButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = onSkip
            )
        }
    }
}

/** The round tonal button that steps back (mirrors in RTL). */
@Composable
private fun OnboardingBackButton(onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(Dimensions.Component.listItemMinHeight),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.onboarding_back)
        )
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
