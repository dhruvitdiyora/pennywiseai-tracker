package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Building blocks for the rule editor, in the same tonal language as the Add
 * screens: borderless filled fields, primary-container selection chips and a
 * rounded block per condition/action. A field that sits on a block uses the page
 * surface as its fill (an inset well); a field that sits on the page uses the
 * card tone, so neither disappears into its background.
 */

@Composable
private fun ruleFieldColors(onCard: Boolean): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    val container = if (onCard) scheme.surface else scheme.surfaceContainerLow
    return TextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        focusedPlaceholderColor = scheme.onSurfaceVariant,
        unfocusedPlaceholderColor = scheme.onSurfaceVariant,
    )
}

/**
 * A rounded, borderless field for the rule editor. [onCard] picks the inset fill
 * for fields inside a [RuleBlock]; [position] gives stacked fields the
 * grouped-list corners. Pass an `ExposedDropdownMenuBox` anchor through
 * [modifier] and set [readOnly] to use it as a dropdown trigger.
 */
@Composable
internal fun RuleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onCard: Boolean = true,
    position: ListItemPosition = ListItemPosition.Single,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        leadingIcon = leadingIcon?.let { glyph ->
            { Icon(imageVector = glyph, contentDescription = null) }
        },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = position.toShape(),
        colors = ruleFieldColors(onCard),
    )
}

/**
 * One option of a pick-one (or pick-many) chip row: primary container with a
 * check when [selected], a tonal chip otherwise. [onCard] picks the idle fill so
 * the chip stays visible on a [RuleBlock].
 */
@Composable
internal fun RuleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onCard: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = scheme.primaryContainer,
            selectedLabelColor = scheme.onPrimaryContainer,
            selectedLeadingIconColor = scheme.onPrimaryContainer,
            containerColor = if (onCard) scheme.surface else scheme.surfaceContainerLow,
            labelColor = scheme.onSurface,
        ),
        border = null,
    )
}

/** A rounded tonal block holding one condition or one action. */
@Composable
internal fun RuleBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.smd),
            content = content,
        )
    }
}

/**
 * A heading for a group of blocks (When / Then, Quick templates) with an
 * optional small "add" action on the right.
 */
@Composable
internal fun RuleSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    addLabel: String? = null,
    onAdd: (() -> Unit)? = null,
) {
    SectionHeaderV2(
        title = title,
        modifier = modifier,
        topSpacing = Spacing.none,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        action = if (addLabel != null && onAdd != null) {
            { RuleAddButton(label = addLabel, onClick = onAdd) }
        } else {
            null
        },
    )
}

@Composable
private fun RuleAddButton(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(Dimensions.Component.chipHeight),
        contentPadding = PaddingValues(horizontal = Spacing.smd),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
