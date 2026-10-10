package com.pennywiseai.tracker.ui.screens.settings

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.AccentColor
import com.pennywiseai.tracker.data.preferences.AppFont
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.data.preferences.NavBarStyle
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.getCoverGradientColors
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Dawn_Foam
import com.pennywiseai.tracker.ui.theme.Dawn_Foam_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Foam_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Gold
import com.pennywiseai.tracker.ui.theme.Dawn_Gold_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Gold_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Highlight
import com.pennywiseai.tracker.ui.theme.Dawn_Highlight_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Highlight_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Iris
import com.pennywiseai.tracker.ui.theme.Dawn_Iris_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Iris_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Love
import com.pennywiseai.tracker.ui.theme.Dawn_Love_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Love_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Muted
import com.pennywiseai.tracker.ui.theme.Dawn_Muted_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Muted_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Overlay
import com.pennywiseai.tracker.ui.theme.Dawn_Overlay_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Overlay_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Pine
import com.pennywiseai.tracker.ui.theme.Dawn_Pine_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Pine_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Rose
import com.pennywiseai.tracker.ui.theme.Dawn_Rose_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Rose_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Subtle
import com.pennywiseai.tracker.ui.theme.Dawn_Subtle_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Subtle_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Surface
import com.pennywiseai.tracker.ui.theme.Dawn_Surface_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Surface_tertiary
import com.pennywiseai.tracker.ui.theme.Dawn_Text
import com.pennywiseai.tracker.ui.theme.Dawn_Text_secondary
import com.pennywiseai.tracker.ui.theme.Dawn_Text_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Foam
import com.pennywiseai.tracker.ui.theme.RosePine_Foam_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Foam_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Gold
import com.pennywiseai.tracker.ui.theme.RosePine_Gold_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Gold_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Highlight
import com.pennywiseai.tracker.ui.theme.RosePine_Highlight_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Highlight_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Iris
import com.pennywiseai.tracker.ui.theme.RosePine_Iris_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Iris_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Love
import com.pennywiseai.tracker.ui.theme.RosePine_Love_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Love_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Muted
import com.pennywiseai.tracker.ui.theme.RosePine_Muted_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Muted_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Overlay
import com.pennywiseai.tracker.ui.theme.RosePine_Overlay_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Overlay_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Pine
import com.pennywiseai.tracker.ui.theme.RosePine_Pine_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Pine_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Rose
import com.pennywiseai.tracker.ui.theme.RosePine_Rose_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Rose_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Subtle
import com.pennywiseai.tracker.ui.theme.RosePine_Subtle_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Subtle_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Surface
import com.pennywiseai.tracker.ui.theme.RosePine_Surface_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Surface_tertiary
import com.pennywiseai.tracker.ui.theme.RosePine_Text
import com.pennywiseai.tracker.ui.theme.RosePine_Text_secondary
import com.pennywiseai.tracker.ui.theme.RosePine_Text_tertiary
import com.pennywiseai.tracker.ui.theme.SNProFontFamily
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

/**
 * Appearance settings, in Cashiro's layout: theme mode, theme style, accent
 * palette and the AMOLED / blur toggles first, then navigation style, cover
 * style and font. The choice rows are rounded connected tiles; the toggles are
 * a grouped list with tonal icon tiles, like the main Settings screen.
 *
 * Cashiro's launcher-logo switcher, Catppuccin palette and navigation label /
 * pill toggles are intentionally not here (see docs/cashiro-port-decisions.md).
 */
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val isDark = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    // The choice tiles and previews carry their own gutter, so the accent and
    // cover rows can scroll to the screen edge instead of stopping at it.
    SettingsSubScreen(
        title = stringResource(R.string.appearance_title),
        backContentDescription = stringResource(R.string.appearance_back),
        onNavigateBack = onNavigateBack,
        horizontalPadding = Spacing.none,
    ) {
        // Theme group: Mode + Style + Accent + AMOLED/Blur toggles
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Theme Mode Selector (System / Light / Dark)
            ThemeModeSelector(
                currentMode = themeUiState.isDarkTheme,
                onModeSelected = { themeViewModel.updateDarkTheme(it) }
            )

            // Theme Style Selector (Dynamic / Branded)
            ThemeStyleSelector(
                currentStyle = themeUiState.themeStyle,
                onStyleSelected = { themeViewModel.updateThemeStyle(it) }
            )

            // Accent Color Picker with ColorSchemeBox (only when branded)
            BlurredAnimatedVisibility(
                visible = themeUiState.themeStyle == ThemeStyle.BRANDED,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.animateContentSize().zIndex(-1f)
            ) {
                LazyRow(
                    modifier = Modifier.selectableGroup(),
                    contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(AccentColor.entries) { accent ->
                        val color = getAccentColorForDisplay(accent, isDark)
                        val secondary = getSecondaryColorForDisplay(accent, isDark)
                        val tertiary = getTertiaryColorForDisplay(accent, isDark)
                        val isSelected = themeUiState.accentColor == accent

                        ColorSchemeBox(
                            accent = color,
                            secondary = secondary,
                            tertiary = tertiary,
                            label = accentColorLabel(accent),
                            onClick = { themeViewModel.updateAccentColor(accent) },
                            isSelected = isSelected
                        )
                    }
                }
            }

            // AMOLED + Blur grouped toggles. Both rows are conditional,
            // so positions are derived from which ones are actually
            // showing — a hand-maintained isFirst/isLast pair got this
            // wrong whenever only one row was visible.
            val showAmoled = themeUiState.isDarkTheme != false
            val showBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val toggleCount = (if (showAmoled) 1 else 0) + (if (showBlur) 1 else 0)
            if (toggleCount > 0) {
                GroupedList(modifier = Modifier.padding(horizontal = Dimensions.Padding.content)) {
                    if (showAmoled) {
                        GlassPreferenceSwitch(
                            title = stringResource(R.string.appearance_amoled_title),
                            subtitle = stringResource(R.string.appearance_amoled_subtitle),
                            checked = themeUiState.isAmoledMode,
                            onCheckedChange = { themeViewModel.updateAmoledMode(it) },
                            leadingIcon = {
                                IconTile(
                                    icon = Icons.Default.DarkMode,
                                    containerColor = if (themeUiState.isAmoledMode) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (themeUiState.isAmoledMode) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            position = ListItemPosition.from(0, toggleCount)
                        )
                    }

                    if (showBlur) {
                        GlassPreferenceSwitch(
                            title = stringResource(R.string.appearance_blur_title),
                            subtitle = stringResource(R.string.appearance_blur_subtitle),
                            checked = themeUiState.blurEffectsEnabled,
                            onCheckedChange = { themeViewModel.updateBlurEffects(it) },
                            leadingIcon = {
                                IconTile(
                                    icon = Icons.Default.BlurOn,
                                    containerColor = if (themeUiState.blurEffectsEnabled) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (themeUiState.blurEffectsEnabled) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            position = ListItemPosition.from(toggleCount - 1, toggleCount)
                        )
                    }
                }
            }
        }

        // Navigation Style Section
        SettingsSection(
            title = stringResource(R.string.appearance_navigation_section),
            headerHorizontalPadding = Dimensions.Padding.content
        ) {
            NavBarStyleSelector(
                currentStyle = themeUiState.navBarStyle,
                onStyleSelected = { themeViewModel.updateNavBarStyle(it) }
            )
        }

        // Cover Style Section
        SettingsSection(
            title = stringResource(R.string.appearance_cover_section),
            headerHorizontalPadding = Dimensions.Padding.content
        ) {
            CoverStyleSelector(
                currentStyle = themeUiState.coverStyle,
                isDark = isDark,
                onStyleSelected = { themeViewModel.updateCoverStyle(it) }
            )
        }

        // Font Selection Section
        SettingsSection(
            title = stringResource(R.string.appearance_fonts_section),
            headerHorizontalPadding = Dimensions.Padding.content
        ) {
            FontSelector(
                currentFont = themeUiState.appFont,
                onFontSelected = { themeViewModel.updateAppFont(it) }
            )
        }
    }
}

// --- Choice tile (shared by every mutually exclusive selector) ---

/**
 * The corner shape of one tile in a row of [count] choice tiles: the row's
 * outer corners are fully rounded and the joins nearly square, matching the
 * grouped-list rows. A single tile (`count == 1`) is fully rounded, so a
 * selector can also render as separate tiles. Start/end corners follow the
 * layout direction.
 */
@Composable
private fun appearanceChoiceShape(index: Int, count: Int): CornerBasedShape {
    val outer = MaterialTheme.shapes.large
    val inner = MaterialTheme.shapes.extraSmall
    return when {
        count <= 1 -> outer
        index == 0 -> outer.copy(topEnd = inner.topEnd, bottomEnd = inner.bottomEnd)
        index == count - 1 -> outer.copy(topStart = inner.topStart, bottomStart = inner.bottomStart)
        else -> inner
    }
}

@Composable
internal fun AppearanceChoiceTile(
    title: String,
    subtitle: String?,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontFamily: FontFamily = FontFamily.Default,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    minHeight: Dp = Dimensions.Component.appearanceChoiceHeight,
) {
    val contentColor = if (selected) {
        selectedContentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = appearanceChoiceShape(index = index, count = count)

    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            // Glass tile; the selected one takes the accent tint and an accent rim.
            .glassPanel(
                shape = shape,
                tint = if (selected) selectedContainerColor else MaterialTheme.colorScheme.surfaceContainerLow,
                rimColor = if (selected) selectedContentColor else null,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.smd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(Dimensions.Icon.inline)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = fontFamily,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

// --- Theme Mode Selector (System / Light / Dark) ---

@Composable
internal fun ThemeModeSelector(
    currentMode: Boolean?,
    onModeSelected: (Boolean?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        data class ModeOption(
            val label: String,
            val icon: ImageVector,
            val value: Boolean?
        )

        val options = listOf(
            ModeOption(stringResource(R.string.appearance_mode_system), Icons.Default.AutoAwesome, null),
            ModeOption(stringResource(R.string.appearance_mode_light), Icons.Default.LightMode, false),
            ModeOption(stringResource(R.string.appearance_mode_dark), Icons.Default.DarkMode, true)
        )

        options.forEachIndexed { index, option ->
            // Cashiro fills the active mode with the solid secondary colour,
            // which is what separates this row from the style tiles below it.
            AppearanceChoiceTile(
                title = option.label,
                subtitle = null,
                selected = currentMode == option.value,
                index = index,
                count = options.size,
                onClick = { onModeSelected(option.value) },
                icon = option.icon,
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedContentColor = MaterialTheme.colorScheme.onSecondary,
                minHeight = Dimensions.Component.listItemMinHeight,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- Theme Style Selector (Dynamic / Branded) ---

@Composable
internal fun ThemeStyleSelector(
    currentStyle: ThemeStyle,
    onStyleSelected: (ThemeStyle) -> Unit
) {
    val options = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                Triple(
                    ThemeStyle.DYNAMIC,
                    R.string.appearance_style_dynamic,
                    R.string.appearance_style_dynamic_subtitle
                )
            )
        }
        add(
            Triple(
                ThemeStyle.BRANDED,
                R.string.appearance_style_default,
                R.string.appearance_style_default_subtitle
            )
        )
    }
    // Two separate rounded tiles (not a joined pair), as in Cashiro.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        options.forEach { (style, title, subtitle) ->
            AppearanceChoiceTile(
                title = stringResource(title),
                subtitle = stringResource(subtitle),
                selected = currentStyle == style,
                index = 0,
                count = 1,
                onClick = { onStyleSelected(style) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- ColorSchemeBox ---

@Composable
private fun accentColorLabel(accent: AccentColor): String = stringResource(
    when (accent) {
        AccentColor.ROSE -> R.string.appearance_accent_rose
        AccentColor.IRIS -> R.string.appearance_accent_iris
        AccentColor.PINE -> R.string.appearance_accent_pine
        AccentColor.GOLD -> R.string.appearance_accent_gold
        AccentColor.LOVE -> R.string.appearance_accent_love
        AccentColor.FOAM -> R.string.appearance_accent_foam
        AccentColor.MUTED -> R.string.appearance_accent_muted
        AccentColor.SUBTLE -> R.string.appearance_accent_subtle
        AccentColor.TEXT -> R.string.appearance_accent_text
        AccentColor.HIGHLIGHT -> R.string.appearance_accent_highlight
        AccentColor.SURFACE -> R.string.appearance_accent_surface
        AccentColor.OVERLAY -> R.string.appearance_accent_overlay
    }
)

@Composable
private fun ColorSchemeBox(
    accent: Color,
    secondary: Color,
    tertiary: Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .size(Dimensions.Component.appearanceAccentPreviewSize)
            .glassPanel(shape = shape, rimColor = if (isSelected) accent else null)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = Spacing.xxs,
                        color = accent.copy(0.7f),
                        shape = shape
                    )
                } else Modifier
            )
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics { contentDescription = label }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.appearance_accent_preview_sample),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Column {
                Box(
                    modifier = Modifier
                        .height(Spacing.md)
                        .width(Spacing.xl)
                        .background(
                            color = tertiary,
                            shape = MaterialTheme.shapes.medium
                        )
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .height(Spacing.md)
                        .width(Spacing.xxl)
                        .background(
                            color = secondary,
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(Dimensions.Icon.inline)
                    .background(
                        accent,
                        MaterialTheme.shapes.extraSmall
                    )
            )
        }
    }
}

// --- Navigation Bar Style Selector ---

@Composable
internal fun NavBarStyleSelector(
    currentStyle: NavBarStyle,
    onStyleSelected: (NavBarStyle) -> Unit
) {
    data class NavOption(
        val style: NavBarStyle,
        val title: Int,
        val subtitle: Int,
        val selectedContainer: Color,
        val selectedContent: Color,
    )

    // Floating takes the tertiary tint and Normal the secondary one, so the
    // two halves of the joined pair read as distinct choices (as in Cashiro).
    val options = listOf(
        NavOption(
            NavBarStyle.FLOATING,
            R.string.appearance_nav_floating,
            R.string.appearance_nav_floating_subtitle,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        ),
        NavOption(
            NavBarStyle.NORMAL,
            R.string.appearance_nav_normal,
            R.string.appearance_nav_normal_subtitle,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        options.forEachIndexed { index, option ->
            AppearanceChoiceTile(
                title = stringResource(option.title),
                subtitle = stringResource(option.subtitle),
                selected = currentStyle == option.style,
                index = index,
                count = options.size,
                onClick = { onStyleSelected(option.style) },
                selectedContainerColor = option.selectedContainer,
                selectedContentColor = option.selectedContent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- Cover Style Selector ---

@Composable
private fun CoverStyleSelector(
    currentStyle: CoverStyle,
    isDark: Boolean,
    onStyleSelected: (CoverStyle) -> Unit
) {
    // The gutter lives in contentPadding (not an outer padding) so the row
    // scrolls under the screen edge like the accent palette above it.
    LazyRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(
            horizontal = Dimensions.Padding.content,
            vertical = Spacing.xs
        )
    ) {
        items(CoverStyle.entries.toList()) { style ->
            val isSelected = currentStyle == style

            Box(
                modifier = Modifier
                    .size(
                        width = Dimensions.Component.appearanceCoverPreviewWidth,
                        height = Dimensions.Component.appearanceCoverPreviewHeight
                    )
                    .clip(MaterialTheme.shapes.large)
                    .then(
                        if (style == CoverStyle.NONE) {
                            Modifier.glassPanel(shape = MaterialTheme.shapes.large)
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = getCoverGradientColors(style, isDark, forPreview = true)
                                )
                            )
                        }
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = Dimensions.Component.selectionStroke,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.large
                        ) else Modifier
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onStyleSelected(style) },
                        role = Role.RadioButton
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (style == CoverStyle.NONE) {
                    Text(
                        text = stringResource(R.string.appearance_cover_none),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.appearance_cover_selected),
                        tint = Color.White,
                        modifier = Modifier.size(Dimensions.Icon.medium)
                    )
                }
            }
        }
    }
}

// --- Font Selector ---

@Composable
internal fun FontSelector(
    currentFont: AppFont,
    onFontSelected: (AppFont) -> Unit
) {
    data class FontOption(
        val font: AppFont,
        val title: Int,
        val subtitle: Int,
        val family: FontFamily,
        val selectedContainer: Color,
        val selectedContent: Color,
    )
    // System keeps the primary tint and SN Pro takes the tertiary one, as in Cashiro.
    val options = listOf(
        FontOption(
            AppFont.SYSTEM,
            R.string.appearance_font_default,
            R.string.appearance_font_default_subtitle,
            FontFamily.Default,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        ),
        FontOption(
            AppFont.SN_PRO,
            R.string.appearance_font_sn_pro,
            R.string.appearance_font_sn_pro_subtitle,
            SNProFontFamily,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        options.forEachIndexed { index, option ->
            AppearanceChoiceTile(
                title = stringResource(option.title),
                subtitle = stringResource(option.subtitle),
                selected = currentFont == option.font,
                index = index,
                count = options.size,
                onClick = { onFontSelected(option.font) },
                fontFamily = option.family,
                selectedContainerColor = option.selectedContainer,
                selectedContentColor = option.selectedContent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- Color lookup functions ---

@Composable
private fun getAccentColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSE -> RosePine_Rose
            AccentColor.IRIS -> RosePine_Iris
            AccentColor.PINE -> RosePine_Pine
            AccentColor.GOLD -> RosePine_Gold
            AccentColor.LOVE -> RosePine_Love
            AccentColor.FOAM -> RosePine_Foam
            AccentColor.MUTED -> RosePine_Muted
            AccentColor.SUBTLE -> RosePine_Subtle
            AccentColor.TEXT -> RosePine_Text
            AccentColor.HIGHLIGHT -> RosePine_Highlight
            AccentColor.SURFACE -> RosePine_Surface
            AccentColor.OVERLAY -> RosePine_Overlay
        }
    } else {
        when (accent) {
            AccentColor.ROSE -> Dawn_Rose
            AccentColor.IRIS -> Dawn_Iris
            AccentColor.PINE -> Dawn_Pine
            AccentColor.GOLD -> Dawn_Gold
            AccentColor.LOVE -> Dawn_Love
            AccentColor.FOAM -> Dawn_Foam
            AccentColor.MUTED -> Dawn_Muted
            AccentColor.SUBTLE -> Dawn_Subtle
            AccentColor.TEXT -> Dawn_Text
            AccentColor.HIGHLIGHT -> Dawn_Highlight
            AccentColor.SURFACE -> Dawn_Surface
            AccentColor.OVERLAY -> Dawn_Overlay
        }
    }
}

@Composable
private fun getSecondaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSE -> RosePine_Rose_secondary
            AccentColor.IRIS -> RosePine_Iris_secondary
            AccentColor.PINE -> RosePine_Pine_secondary
            AccentColor.GOLD -> RosePine_Gold_secondary
            AccentColor.LOVE -> RosePine_Love_secondary
            AccentColor.FOAM -> RosePine_Foam_secondary
            AccentColor.MUTED -> RosePine_Muted_secondary
            AccentColor.SUBTLE -> RosePine_Subtle_secondary
            AccentColor.TEXT -> RosePine_Text_secondary
            AccentColor.HIGHLIGHT -> RosePine_Highlight_secondary
            AccentColor.SURFACE -> RosePine_Surface_secondary
            AccentColor.OVERLAY -> RosePine_Overlay_secondary
        }
    } else {
        when (accent) {
            AccentColor.ROSE -> Dawn_Rose_secondary
            AccentColor.IRIS -> Dawn_Iris_secondary
            AccentColor.PINE -> Dawn_Pine_secondary
            AccentColor.GOLD -> Dawn_Gold_secondary
            AccentColor.LOVE -> Dawn_Love_secondary
            AccentColor.FOAM -> Dawn_Foam_secondary
            AccentColor.MUTED -> Dawn_Muted_secondary
            AccentColor.SUBTLE -> Dawn_Subtle_secondary
            AccentColor.TEXT -> Dawn_Text_secondary
            AccentColor.HIGHLIGHT -> Dawn_Highlight_secondary
            AccentColor.SURFACE -> Dawn_Surface_secondary
            AccentColor.OVERLAY -> Dawn_Overlay_secondary
        }
    }
}

@Composable
private fun getTertiaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSE -> RosePine_Rose_tertiary
            AccentColor.IRIS -> RosePine_Iris_tertiary
            AccentColor.PINE -> RosePine_Pine_tertiary
            AccentColor.GOLD -> RosePine_Gold_tertiary
            AccentColor.LOVE -> RosePine_Love_tertiary
            AccentColor.FOAM -> RosePine_Foam_tertiary
            AccentColor.MUTED -> RosePine_Muted_tertiary
            AccentColor.SUBTLE -> RosePine_Subtle_tertiary
            AccentColor.TEXT -> RosePine_Text_tertiary
            AccentColor.HIGHLIGHT -> RosePine_Highlight_tertiary
            AccentColor.SURFACE -> RosePine_Surface_tertiary
            AccentColor.OVERLAY -> RosePine_Overlay_tertiary
        }
    } else {
        when (accent) {
            AccentColor.ROSE -> Dawn_Rose_tertiary
            AccentColor.IRIS -> Dawn_Iris_tertiary
            AccentColor.PINE -> Dawn_Pine_tertiary
            AccentColor.GOLD -> Dawn_Gold_tertiary
            AccentColor.LOVE -> Dawn_Love_tertiary
            AccentColor.FOAM -> Dawn_Foam_tertiary
            AccentColor.MUTED -> Dawn_Muted_tertiary
            AccentColor.SUBTLE -> Dawn_Subtle_tertiary
            AccentColor.TEXT -> Dawn_Text_tertiary
            AccentColor.HIGHLIGHT -> Dawn_Highlight_tertiary
            AccentColor.SURFACE -> Dawn_Surface_tertiary
            AccentColor.OVERLAY -> Dawn_Overlay_tertiary
        }
    }
}
