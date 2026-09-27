package com.pennywiseai.tracker.ui.screens.settings

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.AccentColor
import com.pennywiseai.tracker.data.preferences.AppFont
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.data.preferences.NavBarStyle
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PreferenceSwitch
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.getCoverGradientColors
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import com.pennywiseai.tracker.ui.effects.overScrollVertical
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()

    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.appearance_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Theme section: Mode + Style + Accent + AMOLED/Blur toggles
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
                        val isDark = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

                        LazyRow(
                            contentPadding = PaddingValues(Spacing.md),
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
                    GroupedList {
                        if (showAmoled) {
                            PreferenceSwitch(
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
                            PreferenceSwitch(
                                title = stringResource(R.string.appearance_blur_title),
                                subtitle = stringResource(R.string.appearance_blur_subtitle),
                                checked = themeUiState.blurEffectsEnabled,
                                onCheckedChange = { themeViewModel.updateBlurEffects(it) },
                                position = ListItemPosition.from(toggleCount - 1, toggleCount)
                            )
                        }
                    }
                }

                // Navigation Style Section
                SectionHeaderV2(
                    title = stringResource(R.string.appearance_navigation_section),
                    modifier = Modifier.padding(start = Dimensions.Padding.content)
                )
                NavBarStyleSelector(
                    currentStyle = themeUiState.navBarStyle,
                    onStyleSelected = { themeViewModel.updateNavBarStyle(it) }
                )

                // Cover Style Section
                SectionHeaderV2(
                    title = stringResource(R.string.appearance_cover_section),
                    modifier = Modifier.padding(start = Dimensions.Padding.content)
                )
                CoverStyleSelector(
                    currentStyle = themeUiState.coverStyle,
                    isDark = themeUiState.isDarkTheme ?: isSystemInDarkTheme(),
                    onStyleSelected = { themeViewModel.updateCoverStyle(it) }
                )

                // Font Selection Section
                SectionHeaderV2(
                    title = stringResource(R.string.appearance_fonts_section),
                    modifier = Modifier.padding(start = Dimensions.Padding.content)
                )
                FontSelector(
                    currentFont = themeUiState.appFont,
                    onFontSelected = { themeViewModel.updateAppFont(it) }
                )

                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}

// --- Navigation back button ---

@Composable
private fun NavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = Dimensions.Padding.content)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.appearance_back),
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
        }
    }
}

// --- Theme Mode Selector (System / Light / Dark) ---

@Composable
internal fun AppearanceChoiceTile(
    title: String,
    subtitle: String?,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = SegmentedButtonDefaults.itemShape(index = index, count = count)

    Surface(
        modifier = modifier
            .heightIn(min = Dimensions.Component.appearanceChoiceHeight)
            .clip(shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
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
            val icon: androidx.compose.ui.graphics.vector.ImageVector,
            val value: Boolean?
        )

        val options = listOf(
            ModeOption(stringResource(R.string.appearance_mode_system), Icons.Default.AutoAwesome, null),
            ModeOption(stringResource(R.string.appearance_mode_light), Icons.Default.LightMode, false),
            ModeOption(stringResource(R.string.appearance_mode_dark), Icons.Default.DarkMode, true)
        )

        options.forEachIndexed { index, option ->
            AppearanceChoiceTile(
                title = option.label,
                subtitle = null,
                selected = currentMode == option.value,
                index = index,
                count = options.size,
                onClick = { onModeSelected(option.value) },
                icon = option.icon,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        options.forEachIndexed { index, (style, title, subtitle) ->
            AppearanceChoiceTile(
                title = stringResource(title),
                subtitle = stringResource(subtitle),
                selected = currentStyle == style,
                index = index,
                count = options.size,
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
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = Spacing.xxs,
                        color = accent.copy(0.7f),
                        shape = shape
                    )
                } else Modifier
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                text = "Abc",
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
    val options = listOf(
        Triple(
            NavBarStyle.FLOATING,
            R.string.appearance_nav_floating,
            R.string.appearance_nav_floating_subtitle
        ),
        Triple(
            NavBarStyle.NORMAL,
            R.string.appearance_nav_normal,
            R.string.appearance_nav_normal_subtitle
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
    ) {
        options.forEachIndexed { index, (style, title, subtitle) ->
            AppearanceChoiceTile(
                title = stringResource(title),
                subtitle = stringResource(subtitle),
                selected = currentStyle == style,
                index = index,
                count = options.size,
                onClick = { onStyleSelected(style) },
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
    LazyRow(
        modifier = Modifier
            .padding(horizontal = Dimensions.Padding.content)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(vertical = Spacing.xs)
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
                            Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
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
    )
    val options = listOf(
        FontOption(
            AppFont.SYSTEM,
            R.string.appearance_font_default,
            R.string.appearance_font_default_subtitle,
            FontFamily.Default
        ),
        FontOption(
            AppFont.SN_PRO,
            R.string.appearance_font_sn_pro,
            R.string.appearance_font_sn_pro_subtitle,
            SNProFontFamily
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
