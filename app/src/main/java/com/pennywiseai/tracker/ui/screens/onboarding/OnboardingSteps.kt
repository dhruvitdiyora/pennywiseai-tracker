package com.pennywiseai.tracker.ui.screens.onboarding

import android.app.LocaleConfig
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.Collator
import java.util.Locale
import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import androidx.compose.ui.graphics.Color
import com.pennywiseai.tracker.ui.screens.settings.glassRow
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
import com.pennywiseai.tracker.ui.components.cards.GlassCard
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
internal fun OnboardingWelcomeStep(uiState: OnBoardingUiState, viewModel: OnBoardingViewModel) {
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

        // Language and currency (#957). Per-app language is framework-only on
        // Android 13+ (same as Settings); older versions follow the device.
        Spacer(Modifier.height(Spacing.lg))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
        ) {
            val hasLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            if (hasLanguage) {
                OnboardingLanguagePicker(position = ListItemPosition.Top)
            }
            OnboardingPicker(
                fieldLabel = stringResource(R.string.onboarding_currency_title),
                options = viewModel.currencies,
                selected = uiState.selectedCurrency,
                label = ::onboardingCurrencyLabel,
                onSelect = viewModel::selectCurrency,
                supportingText = stringResource(R.string.onboarding_currency_hint),
                position = if (hasLanguage) ListItemPosition.Bottom else ListItemPosition.Single,
            )
        }
    }
}

/** Defaults to the device language; picking one sets the per-app locale and recreates the activity. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun OnboardingLanguagePicker(position: ListItemPosition) {
    val context = LocalContext.current
    val localeManager = remember { context.getSystemService(LocaleManager::class.java) }
    val languages = remember {
        val supported = LocaleConfig(context).supportedLocales ?: LocaleList.getEmptyLocaleList()
        // Debug builds also list the en-XA / ar-XB pseudolocales.
        val locales = (0 until supported.size()).map { supported[it] }.filter { it.country !in PSEUDO_REGIONS }
        val multiRegion = locales.groupBy { it.language }.filterValues { it.size > 1 }.keys
        // Region only where a language has several variants (pt-BR / pt-PT).
        listOf<Pair<Locale?, String>>(null to "") + locales
            .map { it to (if (it.language in multiRegion) it.getDisplayName(it) else it.getDisplayLanguage(it)).replaceFirstChar { c -> c.titlecase(it) } }
            .sortedWith(compareBy(Collator.getInstance()) { it.second })
    }
    val deviceLanguage = stringResource(R.string.onboarding_language_device)
    val current = localeManager.applicationLocales.takeUnless { it.isEmpty }?.get(0)
    OnboardingPicker(
        fieldLabel = stringResource(R.string.settings_language_title),
        options = languages,
        selected = languages.firstOrNull { it.first == current } ?: languages.first(),
        label = { (locale, name) -> if (locale == null) deviceLanguage else name },
        onSelect = { (locale, _) ->
            localeManager.applicationLocales = locale?.let { LocaleList(it) } ?: LocaleList.getEmptyLocaleList()
        },
        position = position,
    )
}

/** A read-only tonal field that opens a menu of [options], in the grouped field style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OnboardingPicker(
    fieldLabel: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    position: ListItemPosition,
    supportingText: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        com.pennywiseai.tracker.presentation.people.TonalTextField(
            value = label(selected),
            onValueChange = {},
            label = fieldLabel,
            readOnly = true,
            supportingText = supportingText,
            position = position,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

/** "₹ INR · Indian Rupee" — the localized name helps pick among similar codes. */
private fun onboardingCurrencyLabel(code: String): String {
    val name = runCatching { java.util.Currency.getInstance(code).displayName }.getOrNull()
    val symbol = CurrencyFormatter.getCurrencySymbol(code)
    val base = if (symbol == code) code else "$symbol $code"
    return if (name == null || name == code) base else "$base · $name"
}

private val PSEUDO_REGIONS = setOf("XA", "XB")

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
            // A glass bezel (rim + sheen) in place of the flat hairline frame.
            .glassPanel(shape = MaterialTheme.shapes.extraLarge, tint = scheme.surfaceContainerHigh)
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
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
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
        modifier = Modifier
            .fillMaxWidth()
            .glassRow(position, tint = container, rimColor = if (selected) scheme.primary else null),
        color = Color.Transparent,
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
