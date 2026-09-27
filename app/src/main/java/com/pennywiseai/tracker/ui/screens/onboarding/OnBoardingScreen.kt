package com.pennywiseai.tracker.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Route-level owner for the onboarding ViewModel.
 *
 * The refreshed presentation lives separately so this entry point retains the
 * existing state machine, persistence, permission, and completion ownership.
 */
@Composable
fun OnBoardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnBoardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingRefreshLayout(
        uiState = uiState,
        viewModel = viewModel,
        onComplete = onOnboardingComplete,
    )
}
