package com.pennywiseai.tracker.presentation.add

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.presentation.transactions.txnGlass
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Card as CardIconax
import com.pennywiseai.tracker.ui.icons.iconax.Danger
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Wallet3
import com.pennywiseai.tracker.ui.icons.iconax.WalletMoney
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Shared building blocks for the Add Transaction / Add Subscription screens.
 *
 * Both tabs used to carry their own copy of the field colors, account card,
 * date card and save bar. They live here so the two forms can't drift apart —
 * and so the Cashiro-style chrome (hero amount, pill switcher, tonal field
 * cards) is defined once.
 */

/** Filled, indicator-less text field colors shared by every field on the add screens. */
@Composable
internal fun addFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledIndicatorColor = Color.Transparent,
    disabledLabelColor = MaterialTheme.colorScheme.primary,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * Field colors for a [TextField] drawn on glass: every container is clear and
 * [Modifier.addGlassField] paints the frosted surface (fill, sheen, rim) behind
 * it. Errors keep their red label and text; the container stays glass.
 */
@Composable
internal fun addGlassFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.primary,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * The glass material behind an add-form field or tile, in [shape]. An error
 * field takes the error container as its glass tint.
 */
@Composable
internal fun Modifier.addGlassField(shape: Shape, isError: Boolean = false): Modifier = txnGlass(
    shape = shape,
    tint = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    },
)

/** Small heading above a block of controls ("Transaction type"). */
@Composable
internal fun AddSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PennyWiseText.fieldLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() }
    )
}

// ── Pill switcher ─────────────────────────────────────────────────────────

/**
 * Cashiro's sliding pill switcher: a tonal track with a raised thumb that glides
 * to the selected option. Each option is a `selectable` tab so it reads as
 * selected/not-selected to accessibility services and to tests.
 */
@Composable
internal fun AddPillSwitcher(
    options: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackShape = MaterialTheme.shapes.large
    val thumbShape = MaterialTheme.shapes.medium

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .txnGlass(trackShape, tint = MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(Spacing.xs)
    ) {
        val thumbWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = thumbWidth * selectedIndex,
            animationSpec = tween(durationMillis = Dimensions.Animation.medium),
            label = "pillThumbOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(thumbWidth)
                .height(Dimensions.Component.minTouchTarget)
                .clip(thumbShape)
                .background(MaterialTheme.colorScheme.surface)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimensions.Component.minTouchTarget)
                        .clip(thumbShape)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onIndexChange(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Choice chips ──────────────────────────────────────────────────────────

/** One option of a "pick exactly one" chip row (transaction type, subscription direction). */
@Composable
internal fun AddChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
            }
        } else {
            null
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            // Cashiro: a translucent tonal pill, so the page shows through.
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = CHOICE_CHIP_ALPHA),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null
    )
}

// ── Amount hero ───────────────────────────────────────────────────────────

/**
 * The big, centered amount. It is a real text field, so keyboard entry and paste
 * keep working exactly as before; the calculator chip opens the exact-expression
 * sheet, and the currency chip switches the entry currency. The currency symbol
 * is rendered as a prefix of the field itself so the figure is never untagged.
 */
@Composable
internal fun AddAmountHero(
    amount: String,
    currency: String,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onOpenCalculator: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
    val isPlaceholder = amount.isEmpty() || amount.toBigDecimalOrNull()?.signum() == 0
    val contentColor = when {
        error != null -> MaterialTheme.colorScheme.error
        isPlaceholder -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val textStyle = heroAmountStyle(displayLength = symbol.length + amount.length)
        .copy(color = contentColor, textAlign = TextAlign.Center)
    val prefixTransformation = remember(symbol) { CurrencyPrefixTransformation(symbol) }
    val amountDescription = stringResource(R.string.add_amount_label)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
                .semantics { contentDescription = amountDescription },
            textStyle = textStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = prefixTransformation
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddCurrencyChip(currency = currency, onCurrencyChange = onCurrencyChange)
            AddCalculatorChip(onClick = onOpenCalculator)
        }
    }
}

/** Steps the hero down a type role as the figure gets longer, so it never clips. */
@Composable
private fun heroAmountStyle(displayLength: Int): TextStyle {
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
 * muted "₹0"-style placeholder with the caret sitting between symbol and zero.
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

@Composable
private fun addChipColors() = AssistChipDefaults.assistChipColors(
    // The glass pill behind the chip ([addGlassField]) is its container.
    containerColor = Color.Transparent,
    labelColor = MaterialTheme.colorScheme.onSurface,
    leadingIconContentColor = MaterialTheme.colorScheme.primary,
    trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun AddCurrencyChip(
    currency: String,
    onCurrencyChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = "${CurrencyFormatter.getCurrencySymbol(currency)} $currency"
    val description = stringResource(R.string.add_currency_picker_desc, label)

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            modifier = Modifier
                .addGlassField(CircleShape)
                .semantics { contentDescription = description },
            trailingIcon = {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            shape = CircleShape,
            colors = addChipColors(),
            border = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CurrencyFormatter.getSupportedCurrencies().forEach { code ->
                DropdownMenuItem(
                    text = { Text("${CurrencyFormatter.getCurrencySymbol(code)} $code") },
                    onClick = {
                        onCurrencyChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddCalculatorChip(onClick: () -> Unit) {
    val description = stringResource(R.string.add_open_calculator)
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.add_calculator_chip)) },
        modifier = Modifier
            .addGlassField(CircleShape)
            .semantics { contentDescription = description },
        leadingIcon = {
            Icon(
                Icons.Default.Calculate,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        shape = CircleShape,
        colors = addChipColors(),
        border = null
    )
}

// ── Date / time ───────────────────────────────────────────────────────────

/** Tonal date tile: small year over "dd MMMM", with the calendar glyph. */
@Composable
internal fun AddDateCard(
    dayLabel: String,
    yearLabel: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .addGlassField(MaterialTheme.shapes.large)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .heightIn(min = Dimensions.Component.listItemMinHeight)
            .padding(horizontal = Dimensions.Padding.cardCompact, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = Iconax.Calendar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = yearLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Hour / minute chips plus AM-PM; the whole tile opens the time picker. */
@Composable
internal fun AddTimeCard(
    dateTime: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute
    val amPm = dateTime.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(
                onClickLabel = stringResource(R.string.add_select_time),
                role = Role.Button,
                onClick = onClick
            )
            .heightIn(min = Dimensions.Component.minTouchTarget)
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // A clock reads hour : minute left to right in every locale, so the
        // digits keep LTR order even when the screen itself is mirrored.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeDigits(
                    text = String.format(Locale.getDefault(), "%02d", hour),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = HOUR_CHIP_ALPHA),
                    contentColor = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = ":",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = PennyWiseText.amountMedium
                )
                TimeDigits(
                    text = String.format(Locale.getDefault(), "%02d", minute),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = amPm,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun TimeDigits(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .padding(Spacing.xs)
            .background(color = containerColor, shape = MaterialTheme.shapes.small)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = PennyWiseText.amountMedium,
            modifier = Modifier.padding(Spacing.xs)
        )
    }
}

// ── Account picker card ───────────────────────────────────────────────────

/**
 * Tappable account card used by the single-account picker, both legs of a
 * TRANSFER and the subscription's funding account. Shows the selected account
 * (alias when set, else bank name, with a ••last4 subtitle) or [placeholder],
 * with a clear button when an account is set. The alias line matches the
 * account sheet so the account doesn't change name the moment the sheet
 * closes (#637).
 */
@Composable
internal fun AddAccountSelectorCard(
    account: AccountBalanceEntity?,
    placeholder: String,
    shape: Shape,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .addGlassField(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine)
                .padding(
                    horizontal = Dimensions.Padding.cardCompact,
                    vertical = Spacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            AccountLeadingIcon(account)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account?.let { it.alias?.takeIf { a -> a.isNotBlank() } ?: it.bankName }
                        ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (account != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (account != null &&
                    account.accountLast4 != AccountBalanceEntity.WALLET_ACCOUNT_MARKER
                ) {
                    Text(
                        text = "••${account.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (account != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(Dimensions.Component.minTouchTarget)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.add_clear_account),
                        modifier = Modifier.size(Dimensions.Icon.inline),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The bank's brand logo when we have one, otherwise a type-based glyph. (Without
 * the guard, an unknown bank would borrow an unrelated category icon.)
 */
@Composable
private fun AccountLeadingIcon(account: AccountBalanceEntity?) {
    if (account != null && BrandIcons.getIconResource(account.bankName) != null) {
        BrandIcon(
            merchantName = account.bankName,
            size = Dimensions.Icon.medium,
            showBackground = false
        )
        return
    }
    Icon(
        imageVector = when (account?.getAccountType()) {
            AccountType.CASH -> Iconax.WalletMoney
            AccountType.CREDIT -> Iconax.CardIconax
            AccountType.SAVINGS, AccountType.CURRENT, null -> Iconax.Wallet3
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** The swap badge that sits on the seam between a transfer's From and To cards. */
@Composable
internal fun AddTransferBadge(modifier: Modifier = Modifier) {
    // The outer ring in the page color cuts a notch out of the two cards.
    Box(
        modifier = modifier
            .size(Dimensions.Icon.large + Spacing.sm)
            .background(MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.large)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
        }
    }
}

// ── Feedback + save ───────────────────────────────────────────────────────

@Composable
internal fun AddErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .addGlassField(MaterialTheme.shapes.large, isError = true),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.Padding.cardCompact),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Iconax.Danger,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(Dimensions.Icon.medium)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/** Cashiro's translucent unselected choice chip. */
private const val CHOICE_CHIP_ALPHA = 0.7f

/** Cashiro's hour chip: a primary wash behind primary digits. */
private const val HOUR_CHIP_ALPHA = 0.2f

/** Height of [AddSaveBar] above the navigation bar: fade + button + bottom gap. */
private val AddSaveBarHeight = Spacing.lg + Dimensions.Component.listItemMinHeight + Spacing.sm

/**
 * The last child of a form drawn under [AddSaveBar]: the bar's full height,
 * navigation bar included, so the final field (Notes) scrolls clear of the
 * pinned button instead of hiding behind it.
 */
@Composable
internal fun AddSaveBarClearance() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(AddSaveBarHeight)
    )
}

/**
 * Sticky Save button pinned to the bottom of the form. A short fade above it
 * dissolves scrolled content into the page background, and the button itself
 * sits on a solid band so a disabled (translucent) button never shows the
 * fields scrolling behind it.
 */
@Composable
internal fun BoxScope.AddSaveBar(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    label: String = stringResource(R.string.add_save),
) {
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.lg)
                .background(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, background))
                )
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .navigationBarsPadding()
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    bottom = Spacing.sm
                )
                .height(Dimensions.Component.listItemMinHeight),
            shape = CircleShape
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.Icon.small),
                    strokeWidth = Spacing.xxs
                )
            } else {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
