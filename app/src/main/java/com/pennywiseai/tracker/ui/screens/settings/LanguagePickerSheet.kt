package com.pennywiseai.tracker.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.locale.AppLanguage
import com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.Locale

/**
 * The current in-app language as Settings shows it: the language's own name
 * ("हिन्दी"), or "System default" when the app follows the device.
 */
@Composable
internal fun currentAppLanguageLabel(): String {
    val context = LocalContext.current
    // Re-read after a switch: the configuration changes with the locale.
    val configuration = LocalConfiguration.current
    val current = remember(configuration) { AppLanguage.current(context) }
    val options = remember(current) { AppLanguage.options(current) }
    val selected = AppLanguage.selectedTag(current)
    return options.firstOrNull { it.tag == selected && it.tag != null }?.nativeName
        ?: stringResource(R.string.settings_language_system_default)
}

/**
 * Cashiro's language picker: a sheet with a centred title over a grouped glass
 * list, each language in its own name with a trailing radio. "System default"
 * comes first. Picking a row applies it straight away; the activity is
 * recreated in the new language, which also closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagePickerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val uiLocale = configuration.locales[0] ?: Locale.getDefault()
    val current = remember(configuration) { AppLanguage.current(context) }
    val options = remember(current) { AppLanguage.options(current) }
    val selectedTag = AppLanguage.selectedTag(current)
    val systemDefault = stringResource(R.string.settings_language_system_default)
    val systemDefaultHint = stringResource(R.string.settings_language_system_default_hint)

    PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.settings_language_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.content)
                .padding(top = Spacing.xs, bottom = Spacing.md),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = LanguageListMaxHeight)
                .selectableGroup(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                bottom = Spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
        ) {
            itemsIndexed(options, key = { _, option -> option.tag ?: "system" }) { index, option ->
                val isSelected = option.tag == selectedTag
                val title = if (option.tag == null) systemDefault else option.nativeName
                // The name in the current UI language, when it reads differently
                // ("Hindi" under हिन्दी) — helps when the screen is in a script
                // the user can't read.
                val subtitle = if (option.tag == null) {
                    systemDefaultHint
                } else {
                    val locale = Locale.forLanguageTag(option.tag)
                    val withRegion = options.count {
                        it.tag != null && Locale.forLanguageTag(it.tag).language == locale.language
                    } > 1
                    val local = if (withRegion) locale.getDisplayName(uiLocale) else locale.getDisplayLanguage(uiLocale)
                    local.replaceFirstChar { it.titlecase(uiLocale) }.takeIf { it != title }
                }
                val choose = {
                    if (isSelected) {
                        onDismiss()
                    } else {
                        context.findActivity()?.let { AppLanguage.set(it, option.tag) }
                        onDismiss()
                    }
                }
                GlassGroupedRow(
                    position = ListItemPosition.from(index, options.size),
                    onClick = choose,
                    modifier = Modifier.semantics {
                        role = Role.RadioButton
                        selected = isSelected
                    },
                ) {
                    RowLabels(title = title, subtitle = subtitle)
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding())
    }
}

private val LanguageListMaxHeight = 520.dp

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
