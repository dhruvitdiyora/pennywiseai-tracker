package com.pennywiseai.tracker.presentation.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.presentation.people.TonalTextField
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Information
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
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        // The fields are tonal (surfaceContainerLow), so the sheet sits one step
        // lighter to let them read as raised fields.
        containerColor = MaterialTheme.colorScheme.surface,
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val amount = amountText.trim().toBigDecimalOrNull()
    val currencies = remember { CurrencyFormatter.getSupportedCurrencies().sorted() }

    fun changed() {
        onInputChanged()
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.lend_borrow_entry_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.lend_borrow_entry_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        val directions = listOf(
            LoanDirection.LENT to R.string.lend_borrow_entry_lent,
            LoanDirection.BORROWED to R.string.lend_borrow_entry_borrowed,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            directions.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = direction == option,
                    onClick = {
                        direction = option
                        changed()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = directions.size),
                    modifier = Modifier.heightIn(min = Dimensions.Component.minTouchTarget),
                    enabled = !isSaving,
                    label = {
                        Text(
                            text = stringResource(label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = Dimensions.Alpha.medium),
        ) {
            Row(
                modifier = Modifier.padding(Spacing.smd),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Iconax.Information,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        if (direction == LoanDirection.LENT) {
                            R.string.lend_borrow_entry_hint_lent
                        } else {
                            R.string.lend_borrow_entry_hint_borrowed
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // The amount leads: a large figure with its currency sign, and the
        // currency picker beside it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TonalTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    changed()
                },
                label = stringResource(R.string.lend_borrow_entry_amount),
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = {
                    Text(
                        text = CurrencyFormatter.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                isError = error == LoanEntryError.AMOUNT_REQUIRED ||
                    (amountText.isNotBlank() && (amount == null || amount <= BigDecimal.ZERO)),
            )

            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { if (!isSaving) currencyMenuExpanded = it },
                modifier = Modifier.width(Dimensions.Component.currencySelectorWidth),
            ) {
                TonalTextField(
                    value = currency,
                    onValueChange = {},
                    label = stringResource(R.string.lend_borrow_entry_currency),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    enabled = !isSaving,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(currencyMenuExpanded)
                    },
                    isError = error == LoanEntryError.CURRENCY_REQUIRED,
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false },
                ) {
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                currency = code
                                currencyMenuExpanded = false
                                changed()
                            },
                        )
                    }
                }
            }
        }

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

        Button(
            onClick = {
                onSubmit(
                    personName.trim(),
                    direction,
                    amount,
                    currency,
                    note.trim().ifBlank { null },
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.fab),
            enabled = !isSaving && validateLoanEntry(personName, amount, currency) == null,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                    strokeWidth = Dimensions.Component.progressRingStroke,
                )
            } else {
                Text(
                    text = stringResource(R.string.lend_borrow_entry_save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private val LoanEntryError.messageResource: Int
    get() = when (this) {
        LoanEntryError.PERSON_REQUIRED -> R.string.lend_borrow_entry_person_required
        LoanEntryError.AMOUNT_REQUIRED -> R.string.lend_borrow_entry_amount_required
        LoanEntryError.CURRENCY_REQUIRED -> R.string.lend_borrow_entry_currency_required
        LoanEntryError.SAVE_FAILED -> R.string.lend_borrow_entry_save_failed
    }
