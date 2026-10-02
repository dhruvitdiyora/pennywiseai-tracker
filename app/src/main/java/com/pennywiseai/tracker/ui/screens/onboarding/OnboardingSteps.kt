package com.pennywiseai.tracker.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.Sync
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.screens.FirstRunColumn
import com.pennywiseai.tracker.ui.screens.FirstRunBanner
import com.pennywiseai.tracker.ui.screens.FirstRunHeading
import com.pennywiseai.tracker.ui.screens.FirstRunHero
import com.pennywiseai.tracker.ui.screens.FirstRunHeroGlyphSize
import com.pennywiseai.tracker.ui.screens.FirstRunHeroIcon
import com.pennywiseai.tracker.ui.screens.FirstRunInfoCard
import com.pennywiseai.tracker.ui.screens.FirstRunPermissionRows
import com.pennywiseai.tracker.ui.screens.settings.SettingsIconRow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter

/** The ring behind the welcome hero, drawn at twice the quiet watermark strength. */
private const val WELCOME_RING_ALPHA = Dimensions.Alpha.decorativeWatermark * 2f

// ── Welcome ───────────────────────────────────────────────────────────────

@Composable
internal fun OnboardingWelcomeStep() {
    FirstRunColumn {
        PhonePreviewFrame { WelcomeHeroArt() }
        Spacer(Modifier.height(Spacing.lg))
        FirstRunHeading(
            title = stringResource(R.string.onboarding_welcome_title),
            body = stringResource(R.string.onboarding_welcome_body),
            titleStyle = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(Spacing.lg))
        SetupOverview()
    }
}

/**
 * A neutral device outline that frames the welcome art: a tonal bezel around a
 * screen-coloured pane with a speaker slot. Decorative, so it is hidden from
 * accessibility services.
 */
@Composable
private fun PhonePreviewFrame(content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(Dimensions.Component.onboardingPhoneWidth)
            .height(Dimensions.Component.onboardingPhoneHeight)
            .clearAndSetSemantics { }
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.surfaceContainerHigh)
            .border(
                BorderStroke(Dimensions.Component.hairline, scheme.outlineVariant),
                MaterialTheme.shapes.extraLarge
            )
            .padding(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.background),
            contentAlignment = Alignment.Center
        ) {
            content()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.sm)
                    .width(Spacing.xl)
                    .height(Spacing.xs)
                    .clip(CircleShape)
                    .background(scheme.onSurface.copy(alpha = Dimensions.Alpha.disabled))
            )
        }
    }
}

/** PennyWise's own launcher mark on a tonal disc, inside two quiet rings. */
@Composable
private fun WelcomeHeroArt() {
    Box(
        modifier = Modifier
            .size(Dimensions.Component.onboardingIllustrationSize + Spacing.xl)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = WELCOME_RING_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        FirstRunHero {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private class SetupRow(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

/** "What you'll set up": the four stages ahead, as a grouped block of tonal rows. */
@Composable
private fun SetupOverview() {
    val scheme = MaterialTheme.colorScheme
    val rows = listOf(
        SetupRow(Iconax.Edit2, R.string.onboarding_ui_setup_profile_title, R.string.onboarding_ui_setup_profile_body),
        SetupRow(Iconax.Messages, R.string.onboarding_ui_setup_sms_title, R.string.onboarding_ui_setup_sms_body),
        SetupRow(Iconax.Sync, R.string.onboarding_ui_setup_scan_title, R.string.onboarding_ui_setup_scan_body),
        SetupRow(Iconax.Wallet3, R.string.onboarding_ui_setup_account_title, R.string.onboarding_ui_setup_account_body)
    )
    // Rotate the tonal roles so the four tiles read as a set without a rainbow.
    val tones = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)
    ) {
        SectionHeaderV2(
            title = stringResource(R.string.onboarding_setup_title),
            topSpacing = Spacing.none
        )
        GroupedList {
            rows.forEachIndexed { index, row ->
                val (container, content) = tones[index % tones.size]
                SettingsIconRow(
                    icon = row.icon,
                    iconContainerColor = container,
                    iconContentColor = content,
                    title = stringResource(row.title),
                    subtitle = stringResource(row.body),
                    position = ListItemPosition.from(index, rows.size),
                    subtitleMaxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

// ── Permissions ───────────────────────────────────────────────────────────

@Composable
internal fun OnboardingPermissionsStep(granted: Boolean) {
    FirstRunColumn {
        FirstRunHero {
            FirstRunHeroIcon(icon = if (granted) Icons.Default.Check else Iconax.Messages)
        }
        Spacer(Modifier.height(Spacing.lg))
        FirstRunHeading(
            title = stringResource(R.string.onboarding_permissions_title),
            body = stringResource(R.string.onboarding_permissions_body)
        )
        Spacer(Modifier.height(Spacing.lg))
        FirstRunPermissionRows()
        Spacer(Modifier.height(Spacing.md))
        FirstRunInfoCard(
            title = stringResource(R.string.onboarding_privacy_title),
            body = stringResource(R.string.onboarding_privacy_body)
        )
        if (granted) {
            Spacer(Modifier.height(Spacing.md))
            val incomeColor = MaterialTheme.colorScheme.income
            FirstRunBanner(
                icon = Icons.Default.Check,
                containerColor = incomeColor.copy(alpha = Dimensions.Alpha.tonalIconContainer),
                contentColor = incomeColor,
                message = stringResource(R.string.onboarding_permissions_granted)
            )
        }
    }
}

// ── Scan ──────────────────────────────────────────────────────────────────

@Composable
internal fun OnboardingScanStep(uiState: OnBoardingUiState) {
    FirstRunColumn(verticalArrangement = Arrangement.Center) {
        FirstRunHero {
            when {
                uiState.isScanning -> CircularProgressIndicator(
                    modifier = Modifier.size(FirstRunHeroGlyphSize),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = Dimensions.Component.progressRingStroke
                )
                uiState.scanCompleted -> FirstRunHeroIcon(icon = Icons.Default.Check)
                else -> FirstRunHeroIcon(icon = Iconax.Sync)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        when {
            uiState.isScanning -> {
                FirstRunHeading(title = stringResource(R.string.onboarding_scan_running))
                Spacer(Modifier.height(Spacing.md))
                ScanProgressCard(uiState)
            }
            uiState.scanCompleted -> FirstRunHeading(
                title = stringResource(R.string.onboarding_scan_complete),
                body = if (uiState.scanSaved > 0) {
                    pluralStringResource(
                        R.plurals.onboarding_scan_saved,
                        uiState.scanSaved,
                        uiState.scanSaved,
                        uiState.scanTotal
                    )
                } else {
                    stringResource(R.string.onboarding_scan_none_found)
                }
            )
            else -> FirstRunHeading(
                title = stringResource(R.string.onboarding_scan_title),
                body = stringResource(R.string.onboarding_scan_body)
            )
        }
    }
}

/** Live scan numbers: a progress bar with processed / found / remaining lines. */
@Composable
private fun ScanProgressCard(uiState: OnBoardingUiState) {
    val barModifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.Component.progressBarHeight)
        .clip(CircleShape)
    PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (uiState.scanTotal > 0) {
                val progress = (uiState.scanProcessed.toFloat() / uiState.scanTotal).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progress }, modifier = barModifier)
                Text(
                    text = pluralStringResource(
                        R.plurals.onboarding_scan_processed,
                        uiState.scanProcessed,
                        uiState.scanProcessed,
                        uiState.scanTotal
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.scanParsed > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.onboarding_scan_found,
                            uiState.scanParsed,
                            uiState.scanParsed
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (uiState.scanEstimatedRemaining > 0) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_scan_remaining,
                            uiState.scanEstimatedRemaining / 1000
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LinearProgressIndicator(modifier = barModifier)
                Text(
                    text = stringResource(R.string.onboarding_scan_preparing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Main account ──────────────────────────────────────────────────────────

@Composable
internal fun OnboardingAccountStep(uiState: OnBoardingUiState, onSelect: (String) -> Unit) {
    val empty = uiState.accounts.isEmpty()
    FirstRunColumn(verticalArrangement = if (empty) Arrangement.Center else Arrangement.Top) {
        FirstRunHero(discColor = MaterialTheme.colorScheme.tertiaryContainer) {
            FirstRunHeroIcon(
                icon = if (empty) Icons.Default.Check else Iconax.Wallet3,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        if (empty) {
            FirstRunHeading(
                title = stringResource(R.string.onboarding_account_empty_title),
                body = stringResource(R.string.onboarding_account_empty_body)
            )
        } else {
            FirstRunHeading(
                title = stringResource(R.string.onboarding_account_title),
                body = stringResource(R.string.onboarding_account_body)
            )
            Spacer(Modifier.height(Spacing.lg))
            GroupedList {
                uiState.accounts.forEachIndexed { index, account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    AccountOption(
                        account = account,
                        selected = uiState.selectedAccountKey == key,
                        position = ListItemPosition.from(index, uiState.accounts.size),
                        onClick = { onSelect(key) }
                    )
                }
            }
        }
    }
}

/**
 * One selectable account. The whole row is the radio target; the selected row
 * switches to the primary container with every glyph and label in the matching
 * `on…` role so it stays legible in light and dark.
 */
@Composable
private fun AccountOption(
    account: AccountBalanceEntity,
    selected: Boolean,
    position: ListItemPosition,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (selected) scheme.primaryContainer else scheme.surfaceContainerLow
    val content = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    val muted = if (selected) {
        scheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
    } else {
        scheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        contentColor = content,
        shape = position.toShape()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .defaultMinSize(minHeight = Dimensions.Component.listItemMinHeightTwoLine)
                .padding(horizontal = Spacing.md, vertical = Dimensions.Padding.listRowVertical),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountLeading(account = account, selected = selected)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = account.bankName,
                    style = PennyWiseText.rowTitle,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER) {
                    Text(
                        text = stringResource(R.string.onboarding_account_masked, account.accountLast4),
                        style = PennyWiseText.amountSmall,
                        color = muted
                    )
                }
            }
            Text(
                text = CurrencyFormatter.formatCurrency(account.balance, account.currency),
                style = PennyWiseText.amountRow,
                color = content
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.onboarding_selected),
                    tint = content,
                    modifier = Modifier.size(Dimensions.Icon.inline)
                )
            }
        }
    }
}

/** The bank's logo when we have one, otherwise a wallet glyph on a tonal tile. */
@Composable
private fun AccountLeading(account: AccountBalanceEntity, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    if (BrandIcons.getIconResource(account.bankName) != null) {
        // The name beside it already labels the logo.
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            BrandIcon(merchantName = account.bankName, size = Dimensions.Icon.avatarLarge)
        }
    } else {
        IconTile(
            icon = Iconax.Wallet3,
            containerColor = if (selected) {
                scheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.tonalIconContainer)
            } else {
                scheme.secondaryContainer
            },
            contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSecondaryContainer
        )
    }
}
