package com.pennywiseai.tracker.presentation.budgetgroups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.cards.budgetColorWash
import com.pennywiseai.tracker.presentation.add.addFieldColors
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayTextStyle

/*
 * Building blocks for the budget editor, in the same Cashiro-style vocabulary as
 * the Add screens: a big amount, tonal rounded fields, choice chips, grouped
 * rows and a sticky save bar. The screen itself stays a full screen (its
 * navigation is unchanged); only the look of the form lives here.
 */

private val AMOUNT_INPUT = Regex("^\\d*\\.?\\d*$")

// ── Amount hero ───────────────────────────────────────────────────────────

/**
 * The budget's big, centred amount inside a card washed with the budget's own
 * colour, so picking a colour previews right here. It is a real text field:
 * typing and pasting behave exactly as before, and only plain decimals are
 * accepted. The currency symbol is a visual prefix of the field, so the figure
 * is never shown untagged.
 */
@Composable
internal fun BudgetAmountHero(
    amount: String,
    currency: String,
    color: Color,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
    val isPlaceholder = amount.isEmpty() || amount.toBigDecimalOrNull()?.signum() == 0
    val textStyle = budgetHeroStyle(displayLength = symbol.length + amount.length).copy(
        color = if (isPlaceholder) scheme.onSurfaceVariant else scheme.onSurface,
        textAlign = TextAlign.Center,
    )
    val prefixTransformation = remember(symbol) { CurrencyPrefixTransformation(symbol) }
    val label = stringResource(R.string.budget_edit_amount_label)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        rimColor = color,
        // The wash is painted by the column below so it covers the whole card.
        contentPadding = Dimensions.Padding.none,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .budgetColorWash(color)
                .padding(horizontal = Dimensions.Padding.card, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Component.legendDot)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurfaceVariant,
                )
            }

            BasicTextField(
                value = amount,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(AMOUNT_INPUT)) onAmountChange(value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label },
                textStyle = textStyle,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = prefixTransformation,
            )
        }
    }
}

/** Steps the hero down a type role as the figure grows, so it never clips. */
@Composable
private fun budgetHeroStyle(displayLength: Int): TextStyle {
    val typography = MaterialTheme.typography
    val base = when {
        displayLength <= 8 -> typography.displayMedium
        displayLength <= 11 -> typography.displaySmall
        displayLength <= 14 -> typography.headlineLarge
        displayLength <= 18 -> typography.headlineMedium
        else -> typography.headlineSmall
    }
    // tnum: tabular figures, so digits don't shimmer in width as they are typed.
    return base.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
}

/**
 * Prefixes the field's text with the currency symbol. An empty field renders a
 * muted "₹0"-style placeholder with the caret between symbol and zero.
 */
private class CurrencyPrefixTransformation(private val symbol: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = if (text.isEmpty()) "${symbol}0" else symbol + text.text
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + symbol.length
            override fun transformedToOriginal(offset: Int): Int =
                (offset - symbol.length).coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}

// ── Chrome ────────────────────────────────────────────────────────────────

/**
 * The round tonal delete button for the large top bar's trailing slot: the
 * counterpart of `TonalNavigationButton`, with the glyph in the error colour so
 * the destructive action is still recognisable.
 */
@Composable
internal fun BudgetDeleteActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(end = Dimensions.Padding.content),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimensions.Icon.inline),
        )
    }
}

/**
 * Sticky Save button over a short fade, pinned to the bottom of the form (the
 * budget editor's counterpart of the Add screens' save bar, with a label that
 * says Create or Save). It slides away while [visible] is false so it never
 * sits on top of the keyboard.
 */
@Composable
internal fun BoxScope.BudgetSaveBar(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    visible: Boolean,
    onClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, background, background),
                    ),
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Spacing.lg,
                        bottom = Spacing.sm,
                    )
                    .fillMaxWidth()
                    .height(Dimensions.Component.listItemMinHeight),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Icon.small),
                        strokeWidth = Spacing.xxs,
                    )
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(text = label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ── Period form ───────────────────────────────────────────────────────────

/**
 * Localised full name for a [DayOfWeek] from its `value` (1=Mon..7=Sun),
 * clamping out-of-range inputs. Uses the app locale, not `Locale.getDefault()`,
 * so it matches the per-app language.
 */
@Composable
@ReadOnlyComposable
internal fun dayOfWeekName(value: Int): String =
    DayOfWeek.of(value.coerceIn(1, 7))
        .getDisplayName(DayTextStyle.FULL, LocalConfiguration.current.locales[0])

/**
 * Weekly cadence: a tonal read-only field that opens a menu of weekdays. Selecting
 * a day calls [onWeekdaySelected], which the view model turns into this week's
 * start..end window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetWeekdayField(
    weekStartDay: Int,
    onWeekdaySelected: (Int) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            value = dayOfWeekName(weekStartDay),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = {
                Text(
                    text = stringResource(R.string.budget_edit_week_starts_on),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            leadingIcon = { Icon(Iconax.Calendar, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = shape,
            colors = addFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            (1..7).forEach { day ->
                DropdownMenuItem(
                    text = { Text(dayOfWeekName(day)) },
                    onClick = {
                        onWeekdaySelected(day)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Monthly cadence: the day of the month the cycle starts on, with a – / +
 * stepper for 1..31. The resolver handles the Feb-30/31 case at read time.
 */
@Composable
internal fun BudgetMonthDayRow(
    monthStartDay: Int,
    onMonthDaySelected: (Int) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val stepperColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
            .padding(horizontal = Dimensions.Padding.cardCompact, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        Icon(
            imageVector = Iconax.Calendar,
            contentDescription = null,
            tint = scheme.onSurface,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.budget_edit_month_starts_on),
                style = PennyWiseText.fieldLabel,
                color = scheme.primary,
            )
            Text(
                text = monthStartDay.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
        }
        FilledTonalIconButton(
            onClick = { onMonthDaySelected(monthStartDay - 1) },
            enabled = monthStartDay > 1,
            colors = stepperColors,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.budget_edit_decrease_day),
            )
        }
        FilledTonalIconButton(
            onClick = { onMonthDaySelected(monthStartDay + 1) },
            enabled = monthStartDay < 31,
            colors = stepperColors,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.budget_edit_increase_day),
            )
        }
    }
}

/** One-time cadence: a tonal tile that opens the date picker. Used for start and end. */
@Composable
internal fun BudgetDateRow(
    label: String,
    date: LocalDate,
    formatter: DateTimeFormatter,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
            .padding(horizontal = Dimensions.Padding.cardCompact, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        Icon(
            imageVector = Iconax.Calendar,
            contentDescription = null,
            tint = scheme.onSurface,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = PennyWiseText.fieldLabel,
                color = scheme.primary,
            )
            Text(
                text = date.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
        }
    }
}

/**
 * Read-only "current window" row that closes the period group: the window the
 * budget will track *now* — the same logic the home card and widget use at read
 * time.
 */
@Composable
internal fun BudgetWindowRow(
    caption: String,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.primaryContainer)
            .padding(horizontal = Dimensions.Padding.cardCompact, vertical = Spacing.smd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        Icon(
            imageVector = Iconax.Information,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(Dimensions.Icon.medium),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Text(
                text = stringResource(R.string.budget_edit_current_window),
                style = PennyWiseText.fieldLabel,
                color = scheme.onPrimaryContainer,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimaryContainer,
            )
        }
    }
}

// ── Category limits ───────────────────────────────────────────────────────

/** A category (or type-bucket) glyph on a circle washed with its own colour. */
@Composable
internal fun BudgetCategoryAvatar(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.Icon.avatar,
    glyphSize: Dp = Dimensions.Icon.medium,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                CategoryMapping.colorFor(categoryName).copy(alpha = Dimensions.Alpha.tonalIconContainer),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CategoryIcon(category = categoryName, size = glyphSize)
    }
}

/**
 * One category's limit: its avatar, name and spending so far, a remove button,
 * and the limit field underneath. Stacking the field under the name (rather than
 * squeezing it beside) keeps long names and large fonts readable.
 */
@Composable
internal fun BudgetCategoryLimitRow(
    categoryName: String,
    amount: BigDecimal,
    currentSpending: BigDecimal,
    currency: String,
    position: ListItemPosition,
    onAmountChange: (BigDecimal) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var amountText by remember(amount) {
        mutableStateOf(if (amount.compareTo(BigDecimal.ZERO) == 0) "" else amount.toPlainString())
    }

    GroupedColumn(
        position = position,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = Dimensions.Padding.cardCompact,
            vertical = Spacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
        ) {
            BudgetCategoryAvatar(categoryName = categoryName)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = categoryName,
                    style = PennyWiseText.rowTitle,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentSpending > BigDecimal.ZERO) {
                    Text(
                        text = stringResource(
                            R.string.budget_edit_spent,
                            CurrencyFormatter.formatCurrency(currentSpending, currency),
                        ),
                        style = PennyWiseText.metadata,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.budget_edit_remove),
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    tint = scheme.error,
                )
            }
        }

        TextField(
            value = amountText,
            onValueChange = { value ->
                if (value.isEmpty() || value.matches(AMOUNT_INPUT)) {
                    amountText = value
                    onAmountChange(value.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                }
            },
            prefix = { Text(CurrencyFormatter.getCurrencySymbol(currency)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            // One tonal step above the row, so the field reads as raised from it.
            colors = TextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHigh,
                unfocusedContainerColor = scheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}

/** The category (or type-bucket) entry shown in the "Add category" menu. */
@Composable
internal fun BudgetCategoryMenuItem(
    categoryName: String,
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BudgetCategoryAvatar(
                    categoryName = categoryName,
                    size = Dimensions.Icon.large,
                    glyphSize = Dimensions.Icon.inline,
                )
                Text(label)
            }
        },
        onClick = onClick,
    )
}

/** A wrapping tonal note, used for the "no categories" hint under the limits heading. */
@Composable
internal fun BudgetHintCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = ListItemPosition.Single.toShape(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
