package com.pennywiseai.tracker.ui.screens.onboarding

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter

/** Presentation-only onboarding shell backed by the existing ViewModel contract. */
@Composable
fun OnboardingRefreshLayout(
    uiState: OnBoardingUiState,
    viewModel: OnBoardingViewModel,
    onComplete: () -> Unit
) {
    val stepOrder = remember { OnBoardingStep.entries.toList() }
    val reducedMotion = rememberReducedMotion()
    PennyWiseScaffold(
        customTopBar = {},
        bottomBar = {
            RefreshBottomBar(
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
                onStartScan = viewModel::startSmsScan
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
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                }
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                OnBoardingStep.WELCOME -> RefreshWelcomeStep()
                OnBoardingStep.PROFILE -> RefreshProfileStep(uiState, viewModel)
                OnBoardingStep.PERMISSIONS -> RefreshPermissionsStep(uiState) { viewModel.onSmsPermissionResult(it) }
                OnBoardingStep.SMS_SCAN -> RefreshSmsScanStep(uiState)
                OnBoardingStep.ACCOUNT_SETUP -> RefreshAccountSetupStep(uiState, viewModel::selectAccount)
            }
        }
    }
}

@Composable
private fun RefreshWelcomeStep() {
    RefreshColumn {
        PhonePreviewFrame { PreviewBalanceCard() }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.lg))
        PennyWiseCardV2(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(text = stringResource(R.string.onboarding_setup_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(Spacing.sm))
            Text(text = stringResource(R.string.onboarding_setup_steps), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun PhonePreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .width(Dimensions.Component.onboardingPhoneWidth)
            .height(Dimensions.Component.onboardingPhoneHeight)
            .clearAndSetSemantics { }
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(BorderStroke(Dimensions.Component.hairline, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.extraLarge)
            .padding(Spacing.smd)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(Spacing.xl).height(Spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.medium)))
                Box(Modifier.size(Spacing.sm).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.sm))
            content()
        }
    }
}

@Composable
private fun PreviewBalanceCard() {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = Dimensions.Padding.cardCompact
    ) {
        Text(text = stringResource(R.string.onboarding_preview_balance), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(Spacing.xs))
        Text(text = stringResource(R.string.onboarding_preview_hidden_amount), style = PennyWiseText.amountLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(Spacing.smd))
        Box(Modifier.fillMaxWidth().height(Dimensions.Component.progressBarHeight).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.tonalIconContainer)))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RefreshProfileStep(uiState: OnBoardingUiState, viewModel: OnBoardingViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::selectProfileImage) }
    RefreshColumn {
        ProfilePreview(uiState, viewModel)
        Spacer(Modifier.height(Spacing.lg))
        Text(text = stringResource(R.string.onboarding_profile_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(text = stringResource(R.string.onboarding_profile_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.lg))
        TextField(
            value = uiState.userName,
            onValueChange = viewModel::updateUserName,
            label = { Text(stringResource(R.string.onboarding_profile_name_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(text = stringResource(R.string.onboarding_profile_avatar), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            itemsIndexed(viewModel.avatarDrawables) { index, drawable ->
                val selected = uiState.profileImageUri == null && uiState.selectedAvatarIndex == index
                ProfileOptionTile(selected, { viewModel.selectAvatar(index) }) {
                    Image(painterResource(drawable), stringResource(R.string.onboarding_avatar_description, index + 1), Modifier.size(Dimensions.Icon.avatar), contentScale = ContentScale.Crop)
                }
            }
            item {
                ProfileOptionTile(uiState.profileImageUri != null, { picker.launch("image/*") }) {
                    if (uiState.profileImageUri != null) {
                        AsyncImage(uiState.profileImageUri, stringResource(R.string.onboarding_photo_selected), Modifier.size(Dimensions.Icon.avatar).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Text(text = stringResource(R.string.onboarding_add_photo), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(text = stringResource(R.string.onboarding_profile_background), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            viewModel.backgroundColors.forEachIndexed { index, colorInt ->
                val selected = uiState.selectedBackgroundColor == index
                val description = stringResource(
                    R.string.onboarding_background_description,
                    index + 1,
                )
                Box(
                    Modifier.size(Dimensions.Component.onboardingOptionTile).clip(CircleShape).background(Color(colorInt)).then(
                        if (selected) Modifier.border(Dimensions.Component.onboardingSelectionStroke, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                    ).semantics {
                        contentDescription = description
                        this.selected = selected
                        role = Role.RadioButton
                    }.clickable { viewModel.selectBackgroundColor(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Default.Check, stringResource(R.string.onboarding_selected), tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(Dimensions.Icon.medium))
                }
            }
        }
    }
}

@Composable
private fun ProfilePreview(uiState: OnBoardingUiState, viewModel: OnBoardingViewModel) {
    val bg = viewModel.backgroundColors.getOrNull(uiState.selectedBackgroundColor)?.let(::Color) ?: MaterialTheme.colorScheme.primaryContainer
    PhonePreviewFrame {
        Box(Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large).background(bg), contentAlignment = Alignment.Center) {
            if (uiState.profileImageUri != null) {
                AsyncImage(uiState.profileImageUri, null, Modifier.size(Dimensions.Icon.avatarLarge).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Image(painterResource(viewModel.avatarDrawables.getOrElse(uiState.selectedAvatarIndex) { viewModel.avatarDrawables.first() }), null, Modifier.size(Dimensions.Icon.avatarLarge).clip(CircleShape), contentScale = ContentScale.Crop)
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.sm),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = uiState.userName.ifBlank {
                        stringResource(R.string.onboarding_profile_preview_name)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(
                        horizontal = Spacing.smd,
                        vertical = Spacing.xs,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProfileOptionTile(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(Dimensions.Component.onboardingOptionTile).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (selected) Modifier.border(Dimensions.Component.onboardingSelectionStroke, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun RefreshPermissionsStep(uiState: OnBoardingUiState, onPermissionResult: (Boolean) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onPermissionResult(result[Manifest.permission.READ_SMS] == true)
    }
    RefreshColumn(verticalArrangement = Arrangement.Center) {
        MessageIllustration(uiState.smsPermissionGranted)
        Spacer(Modifier.height(Spacing.lg))
        Text(text = stringResource(R.string.onboarding_permissions_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(text = stringResource(R.string.onboarding_permissions_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.lg))
        PennyWiseCardV2(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(text = stringResource(R.string.onboarding_privacy_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(Spacing.sm))
            Text(text = stringResource(R.string.onboarding_privacy_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(Spacing.lg))
        if (uiState.smsPermissionGranted) {
            val incomeColor = MaterialTheme.colorScheme.income
            PennyWiseCardV2(modifier = Modifier.fillMaxWidth(), containerColor = incomeColor.copy(alpha = if (isSystemInDarkTheme()) 0.15f else 0.12f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = incomeColor)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(text = stringResource(R.string.onboarding_permissions_granted), style = MaterialTheme.typography.bodyMedium, color = incomeColor)
                }
            }
        } else {
            Button(
                onClick = {
                    val permissions = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    launcher.launch(permissions.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.onboarding_permissions_enable)) }
        }
    }
}

@Composable
private fun MessageIllustration(granted: Boolean) {
    Box(Modifier.size(Dimensions.Component.onboardingIllustrationSize).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        Box(Modifier.size(Dimensions.Icon.extraLarge).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(if (granted) Icons.Default.Check else Icons.Default.MailOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimensions.Icon.large))
        }
    }
}

@Composable
private fun RefreshSmsScanStep(uiState: OnBoardingUiState) {
    RefreshColumn(verticalArrangement = Arrangement.Center) {
        ScanIllustration(uiState.isScanning, uiState.scanCompleted)
        Spacer(Modifier.height(Spacing.lg))
        when {
            uiState.isScanning -> {
                Text(text = stringResource(R.string.onboarding_scan_running), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.md))
                if (uiState.scanTotal > 0) {
                    val progress = (uiState.scanProcessed.toFloat() / uiState.scanTotal).coerceIn(0f, 1f)
                    LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(Dimensions.Component.progressBarHeight).clip(CircleShape))
                    Spacer(Modifier.height(Spacing.sm))
                    Text(text = pluralStringResource(R.plurals.onboarding_scan_processed, uiState.scanProcessed, uiState.scanProcessed, uiState.scanTotal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.scanParsed > 0) Text(text = pluralStringResource(R.plurals.onboarding_scan_found, uiState.scanParsed, uiState.scanParsed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    if (uiState.scanEstimatedRemaining > 0) Text(text = stringResource(R.string.onboarding_scan_remaining, uiState.scanEstimatedRemaining / 1000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(Dimensions.Component.progressBarHeight).clip(CircleShape))
                    Spacer(Modifier.height(Spacing.sm))
                    Text(text = stringResource(R.string.onboarding_scan_preparing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            uiState.scanCompleted -> {
                Text(text = stringResource(R.string.onboarding_scan_complete), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = if (uiState.scanSaved > 0) pluralStringResource(R.plurals.onboarding_scan_saved, uiState.scanSaved, uiState.scanSaved, uiState.scanTotal) else stringResource(R.string.onboarding_scan_none_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Text(text = stringResource(R.string.onboarding_scan_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                Text(text = stringResource(R.string.onboarding_scan_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ScanIllustration(scanning: Boolean, complete: Boolean) {
    Box(Modifier.size(Dimensions.Component.onboardingIllustrationSize).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        when {
            scanning -> CircularProgressIndicator(Modifier.size(Dimensions.Icon.extraLarge), color = MaterialTheme.colorScheme.onPrimaryContainer)
            complete -> Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Dimensions.Icon.extraLarge))
            else -> Icon(Icons.Default.MailOutline, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Dimensions.Icon.extraLarge))
        }
    }
}

@Composable
private fun RefreshAccountSetupStep(uiState: OnBoardingUiState, onSelect: (String) -> Unit) {
    RefreshColumn {
        AccountIllustration(uiState.accounts.isEmpty())
        Spacer(Modifier.height(Spacing.lg))
        if (uiState.accounts.isEmpty()) {
            Text(text = stringResource(R.string.onboarding_account_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.sm))
            Text(text = stringResource(R.string.onboarding_account_empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        } else {
            Text(text = stringResource(R.string.onboarding_account_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.sm))
            Text(text = stringResource(R.string.onboarding_account_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.lg))
            uiState.accounts.forEach { account ->
                val key = "${account.bankName}_${account.accountLast4}"
                AccountOption(account, uiState.selectedAccountKey == key) { onSelect(key) }
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun AccountIllustration(empty: Boolean) {
    Box(Modifier.size(Dimensions.Component.onboardingIllustrationSize).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
        Icon(if (empty) Icons.Default.AccountBalance else Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(Dimensions.Icon.extraLarge))
    }
}

@Composable
private fun AccountOption(account: AccountBalanceEntity, selected: Boolean, onClick: () -> Unit) {
    val muted = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.subtitle) else MaterialTheme.colorScheme.onSurfaceVariant
    PennyWiseCardV2(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
        onClick = onClick,
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(Dimensions.Component.hairline, MaterialTheme.colorScheme.primary) else null,
        contentPadding = Dimensions.Padding.cardCompact
    ) {
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimensions.Component.minTouchTarget), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(text = account.bankName, style = MaterialTheme.typography.titleSmall)
                if (account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) Text(text = stringResource(R.string.onboarding_account_masked, account.accountLast4), style = PennyWiseText.amountSmall, color = muted)
            }
            Text(text = CurrencyFormatter.formatCurrency(account.balance, account.currency), style = PennyWiseText.amountRow)
            if (selected) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.Default.Check, stringResource(R.string.onboarding_selected), tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Dimensions.Icon.inline))
            }
        }
    }
}

@Composable
private fun RefreshColumn(verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().overScrollVertical().verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.Padding.content, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
private fun StepIndicator(current: OnBoardingStep, modifier: Modifier = Modifier) {
    val index = OnBoardingStep.entries.indexOf(current)
    val progressDescription = stringResource(
        R.string.onboarding_step_progress,
        index + 1,
        OnBoardingStep.entries.size,
    )
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = progressDescription
        },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnBoardingStep.entries.forEachIndexed { position, step ->
            Box(
                Modifier.padding(horizontal = Spacing.xs).size(if (step == current) Dimensions.Component.onboardingStepActive else Dimensions.Component.onboardingStepInactive).clip(CircleShape).background(
                    when {
                        step == current -> MaterialTheme.colorScheme.primary
                        position < index -> MaterialTheme.colorScheme.primary.copy(alpha = Dimensions.Alpha.medium)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = Dimensions.Alpha.divider)
                    }
                )
            )
        }
    }
}

@Composable
private fun RefreshBottomBar(uiState: OnBoardingUiState, onBack: () -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onStartScan: () -> Unit) {
    val canGoBack = uiState.currentStep != OnBoardingStep.WELCOME && !uiState.isScanning
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)) {
        StepIndicator(uiState.currentStep, Modifier.fillMaxWidth().padding(bottom = Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.onboarding_back))
                }
                Spacer(Modifier.width(Spacing.sm))
            }
            RefreshActions(uiState, onNext, onSkip, onStartScan, if (canGoBack) Modifier.weight(1f) else Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RefreshActions(uiState: OnBoardingUiState, onNext: () -> Unit, onSkip: () -> Unit, onStartScan: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (uiState.currentStep) {
            OnBoardingStep.WELCOME -> PrimaryAction(stringResource(R.string.onboarding_get_started), onNext)
            OnBoardingStep.PROFILE -> PrimaryAction(stringResource(R.string.onboarding_save_continue), onNext, uiState.userName.isNotBlank())
            OnBoardingStep.PERMISSIONS -> if (!uiState.smsPermissionGranted) {
                TextButton(onClick = onSkip, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
            } else PrimaryAction(stringResource(R.string.onboarding_continue), onNext)
            OnBoardingStep.SMS_SCAN -> when {
                !uiState.isScanning && !uiState.scanCompleted -> {
                    TextButton(onClick = onSkip, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
                    PrimaryAction(stringResource(R.string.onboarding_start_scanning), onStartScan)
                }
                uiState.isScanning -> TextButton(onClick = onSkip, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
                else -> PrimaryAction(stringResource(R.string.onboarding_continue), onNext)
            }
            OnBoardingStep.ACCOUNT_SETUP -> {
                if (uiState.accounts.isNotEmpty() && uiState.selectedAccountKey == null) TextButton(onClick = onSkip, Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
                PrimaryAction(stringResource(R.string.onboarding_finish), onNext, uiState.selectedAccountKey != null || uiState.accounts.isEmpty(), uiState.isCompleting)
            }
        }
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true, progress: Boolean = false) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimensions.Component.buttonHeight)) {
        if (progress) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.Icon.medium),
                strokeWidth = Dimensions.Component.progressRingStroke,
            )
        } else {
            Text(text = text)
        }
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
