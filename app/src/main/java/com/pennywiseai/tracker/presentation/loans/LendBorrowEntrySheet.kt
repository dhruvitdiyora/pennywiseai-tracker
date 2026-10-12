package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.PennyWiseBottomSheet
import com.pennywiseai.tracker.ui.components.PennyWiseSegmentedSwitcher
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

internal data class LoanEntryDraft(
    val personName: String = "",
    val direction: LoanDirection = LoanDirection.LENT,
    val amountText: String = "",
    val currency: String = "INR",
    val note: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowEntrySheet(
    initialCurrency: String,
    initialPersonName: String = "",
    isPersonEditable: Boolean = true,
    error: LoanEntryError?,
    isSaving: Boolean,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Cashiro's sheet: a solid `surface` container, so the tonal
    // (surfaceContainerLow) fields read as raised.
    PennyWiseBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        LendBorrowEntryForm(
            initialDraft = LoanEntryDraft(
                personName = initialPersonName,
                currency = initialCurrency,
            ),
            isPersonEditable = isPersonEditable,
            error = error,
            isSaving = isSaving,
            onInputChanged = onInputChanged,
            onSubmit = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.lg,
                ),
        )
    }
}

@Composable
internal fun LendBorrowEntryForm(
    initialDraft: LoanEntryDraft,
    isPersonEditable: Boolean = true,
    error: LoanEntryError?,
    isSaving: Boolean,
    onInputChanged: () -> Unit,
    onSubmit: (
        personName: String,
        direction: LoanDirection,
        amount: BigDecimal?,
        currency: String,
        note: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var personName by remember(initialDraft.personName) {
        mutableStateOf(initialDraft.personName)
    }
    var direction by remember(initialDraft.direction) {
        mutableStateOf(initialDraft.direction)
    }
    var amountText by remember(initialDraft.amountText) {
        mutableStateOf(initialDraft.amountText)
    }
    var currency by remember(initialDraft.currency) {
        mutableStateOf(initialDraft.currency)
    }
    var note by remember(initialDraft.note) { mutableStateOf(initialDraft.note) }
    val amount = amountText.trim().toBigDecimalOrNull()
    val currencies = remember { CurrencyFormatter.getSupportedCurrencies().sorted() }

    fun changed() {
        onInputChanged()
    }

    // Cashiro's AddEditLendBorrowTransactionSheet order: title, type switcher,
    // info card, the big amount, then who and why, then save.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        LoanSheetTitle(
            title = stringResource(R.string.lend_borrow_entry_title),
            subtitle = stringResource(R.string.lend_borrow_entry_description),
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        val directions = listOf(LoanDirection.LENT, LoanDirection.BORROWED)
        PennyWiseSegmentedSwitcher(
            options = listOf(
                stringResource(R.string.lend_borrow_entry_lent),
                stringResource(R.string.lend_borrow_entry_borrowed),
            ),
            selectedIndex = directions.indexOf(direction),
            onIndexChange = { index ->
                if (!isSaving) {
                    direction = directions[index]
                    changed()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LoanInfoCard(
            text = stringResource(
                if (direction == LoanDirection.LENT) {
                    R.string.lend_borrow_entry_hint_lent
                } else {
                    R.string.lend_borrow_entry_hint_borrowed
                },
            ),
        )

        LoanAmountInput(
            amountText = amountText,
            currency = currency,
            currencies = currencies,
            enabled = !isSaving,
            isError = error == LoanEntryError.AMOUNT_REQUIRED ||
                (amountText.isNotBlank() && (amount == null || amount <= BigDecimal.ZERO)),
            onAmountChange = {
                amountText = it
                changed()
            },
            onCurrencyChange = {
                currency = it
                changed()
            },
        )

        // Person and note read as one connected pair of fields.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
            TonalTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    changed()
                },
                label = stringResource(R.string.lend_borrow_entry_person),
                position = ListItemPosition.Top,
                enabled = !isSaving && isPersonEditable,
                placeholder = stringResource(R.string.lend_borrow_entry_person_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = error == LoanEntryError.PERSON_REQUIRED,
            )
            TonalTextField(
                value = note,
                onValueChange = {
                    note = it
                    changed()
                },
                label = stringResource(R.string.lend_borrow_entry_note),
                position = ListItemPosition.Bottom,
                enabled = !isSaving,
                singleLine = false,
                minLines = 2,
                maxLines = 3,
            )
        }

        error?.let {
            Text(
                text = stringResource(it.messageResource),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        LoanSheetButton(
            label = stringResource(R.string.lend_borrow_entry_save),
            onClick = {
                onSubmit(
                    personName.trim(),
                    direction,
                    amount,
                    currency,
                    note.trim().ifBlank { null },
                )
            },
            modifier = Modifier.padding(top = Spacing.xs),
            enabled = !isSaving && validateLoanEntry(personName, amount, currency) == null,
            isLoading = isSaving,
        )
    }
}

/**
 * Cashiro's AmountInput: the figure large, bold and centred on a page-coloured
 * panel with its currency sign in front. It stays a real text field (keyboard
 * and paste work as before), and the currency chip under it switches the entry
 * currency, so the amount is never shown untagged.
 */
@Composable
private fun LoanAmountInput(
    amountText: String,
    currency: String,
    currencies: List<String>,
    enabled: Boolean,
    isError: Boolean,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
    val color = when {
        isError -> scheme.error
        amountText.isBlank() -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val style = amountStyle(symbol.length + amountText.length)
        .copy(color = color, textAlign = TextAlign.Center)
    val prefix = remember(symbol) { AmountPrefixTransformation(symbol) }
    val amountLabel = stringResource(R.string.lend_borrow_entry_amount)
    val currencyLabel = stringResource(R.string.lend_borrow_entry_currency)
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background, RoundedCornerShape(Dimensions.CornerRadius.card))
            .padding(Spacing.smd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BasicTextField(
            value = amountText,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = amountLabel },
            enabled = enabled,
            textStyle = style,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(scheme.primary),
            visualTransformation = prefix,
        )
        Box {
            AssistChip(
                onClick = { currencyMenuExpanded = true },
                enabled = enabled,
                label = { Text("$symbol $currency") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = scheme.surfaceContainerLow,
                    labelColor = scheme.onSurface,
                    trailingIconContentColor = scheme.onSurfaceVariant,
                ),
                border = null,
                modifier = Modifier.semantics { contentDescription = "$currencyLabel, $currency" },
            )
            DropdownMenu(
                expanded = currencyMenuExpanded,
                onDismissRequest = { currencyMenuExpanded = false },
                shape = MaterialTheme.shapes.large,
            ) {
                currencies.forEach { code ->
                    DropdownMenuItem(
                        text = { Text("${CurrencyFormatter.getCurrencySymbol(code)} $code") },
                        onClick = {
                            onCurrencyChange(code)
                            currencyMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

/** Steps the figure down a type role as it grows, so a long amount never clips. */
@Composable
private fun amountStyle(displayLength: Int): TextStyle {
    val typography = MaterialTheme.typography
    val base = when {
        displayLength <= 8 -> typography.displayMedium
        displayLength <= 11 -> typography.displaySmall
        displayLength <= 14 -> typography.headlineLarge
        else -> typography.headlineMedium
    }
    // tnum: tabular figures, so digits don't shimmer in width as they are typed.
    return base.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
}

/** Draws the currency sign in front of the typed figure; an empty field shows a muted "₹0"-style hint. */
private class AmountPrefixTransformation(private val symbol: String) : VisualTransformation {
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

private val LoanEntryError.messageResource: Int
    get() = when (this) {
        LoanEntryError.PERSON_REQUIRED -> R.string.lend_borrow_entry_person_required
        LoanEntryError.AMOUNT_REQUIRED -> R.string.lend_borrow_entry_amount_required
        LoanEntryError.CURRENCY_REQUIRED -> R.string.lend_borrow_entry_currency_required
        LoanEntryError.SAVE_FAILED -> R.string.lend_borrow_entry_save_failed
    }
