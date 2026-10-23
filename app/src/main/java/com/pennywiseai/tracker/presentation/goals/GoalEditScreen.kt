package com.pennywiseai.tracker.presentation.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.add.AddErrorBanner
import com.pennywiseai.tracker.presentation.add.AddSectionLabel
import com.pennywiseai.tracker.presentation.add.addFieldColors
import com.pennywiseai.tracker.presentation.budgetgroups.BudgetSaveBar
import com.pennywiseai.tracker.ui.components.ColorSwatchRow
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconax.Calendar
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Amount fields accept digits with one optional decimal point. */
internal val amountInput = Regex("^\\d*\\.?\\d{0,2}$")

/**
 * New / edit goal, in the budget editor's chrome: large collapsing title,
 * tonal back button, filled tonal fields in connected groups and a sticky
 * save bar, so goals read as part of the Budgets family.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onNavigateBack() }

    val form = state.form
    val errors = if (state.showErrors) form.errors(viewModel.today, state.original) else emptySet()
    val perMonth = form.perMonth(viewModel.today, state.original, state.contributions)
    var showDatePicker by remember { mutableStateOf(false) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(if (state.isEditing) R.string.goal_edit_title_edit else R.string.goal_edit_title_new),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.goal_back),
                    )
                },
                hazeState = hazeState,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + padding.calculateTopPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.Layout.sectionGap),
            ) {
                // Name + due date: what the goal is and when it's for.
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                    TextField(
                        value = form.name,
                        onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                        label = { Text(stringResource(R.string.goal_edit_name), fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text(stringResource(R.string.goal_edit_name_hint)) },
                        leadingIcon = { Icon(Iconax.Edit2, contentDescription = null) },
                        singleLine = true,
                        isError = GoalFieldError.NAME_BLANK in errors,
                        supportingText = errorText(GoalFieldError.NAME_BLANK in errors, R.string.goal_edit_error_name),
                        shape = ListItemPosition.Top.toShape(),
                        colors = addFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GoalDateField(
                        label = stringResource(R.string.goal_edit_due),
                        date = form.dueDate,
                        isError = GoalFieldError.DUE_NOT_AFTER_TODAY in errors,
                        error = stringResource(R.string.goal_edit_error_due),
                        shape = ListItemPosition.Bottom.toShape(),
                        onClick = { showDatePicker = true },
                    )
                }

                // Currency + target (+ already saved for a new goal): the money.
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                    val amountShape = (if (state.isEditing) ListItemPosition.Single else ListItemPosition.Top).toShape()
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)) {
                        CurrencyField(
                            currency = form.currency,
                            enabled = !state.currencyLocked,
                            onSelect = { c -> viewModel.update { it.copy(currency = c) } },
                            shape = amountShape,
                            modifier = Modifier.weight(0.4f),
                        )
                        TextField(
                            value = form.target,
                            onValueChange = { v -> if (v.matches(amountInput)) viewModel.update { it.copy(target = v) } },
                            label = { Text(stringResource(R.string.goal_edit_target), fontWeight = FontWeight.SemiBold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = GoalFieldError.TARGET_INVALID in errors,
                            supportingText = errorText(GoalFieldError.TARGET_INVALID in errors, R.string.goal_edit_error_target),
                            shape = amountShape,
                            colors = addFieldColors(),
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                    if (!state.isEditing) {
                        TextField(
                            value = form.alreadySaved,
                            onValueChange = { v -> if (v.matches(amountInput)) viewModel.update { it.copy(alreadySaved = v) } },
                            label = { Text(stringResource(R.string.goal_edit_already_saved), fontWeight = FontWeight.SemiBold) },
                            prefix = { Text(CurrencyFormatter.getCurrencySymbol(form.currency)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = GoalFieldError.ALREADY_SAVED_INVALID in errors,
                            supportingText = {
                                Text(
                                    stringResource(
                                        if (GoalFieldError.ALREADY_SAVED_INVALID in errors) R.string.goal_edit_error_already_saved
                                        else R.string.goal_edit_already_saved_hint
                                    )
                                )
                            },
                            shape = ListItemPosition.Bottom.toShape(),
                            colors = addFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.currencyLocked) {
                        Text(
                            stringResource(R.string.goal_edit_currency_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xs),
                        )
                    }
                    if (perMonth != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        GoalPerMonthBanner(
                            text = stringResource(R.string.goal_edit_helper, CurrencyFormatter.formatCurrency(perMonth, form.currency)),
                        )
                    }
                }

                if (state.saveFailed) {
                    AddErrorBanner(message = stringResource(R.string.goal_edit_failed))
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
                    AddSectionLabel(text = stringResource(R.string.goal_edit_color))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = Dimensions.Padding.cardCompact,
                    ) {
                        ColorSwatchRow(selected = form.color, onSelect = { c -> viewModel.update { it.copy(color = c) } })
                    }
                }

                // Clears the sticky save bar that floats over the form:
                // fade (lg) + button + bottom gap (sm) + breathing room.
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(Spacing.lg + Dimensions.Component.listItemMinHeight + Spacing.sm + Dimensions.Padding.content)
                )
            }

            BudgetSaveBar(
                label = stringResource(if (state.isEditing) R.string.goal_edit_save else R.string.goal_edit_create),
                enabled = !state.isSaving,
                isLoading = state.isSaving,
                visible = !isKeyboardVisible,
                onClick = viewModel::save,
            )
        }
    }

    if (showDatePicker) {
        val today = viewModel.today
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.dueDate?.let { it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toUtcDate().isAfter(today)
                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms -> viewModel.update { it.copy(dueDate = ms.toUtcDate()) } }
                    showDatePicker = false
                }) { Text(stringResource(R.string.goal_action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.goal_action_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

internal fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun errorText(show: Boolean, res: Int): (@Composable () -> Unit)? =
    if (show) ({ Text(stringResource(res)) }) else null

/**
 * A read-only date field in the add-form style; the whole field opens the
 * date picker, not just an icon.
 */
@Composable
internal fun GoalDateField(
    label: String,
    date: LocalDate?,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    error: String? = null,
) {
    val base = addFieldColors()
    val colors = if (isError) {
        base.copy(
            disabledLabelColor = MaterialTheme.colorScheme.error,
            disabledLeadingIconColor = MaterialTheme.colorScheme.error,
            disabledSupportingTextColor = MaterialTheme.colorScheme.error,
        )
    } else {
        base
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // Disabled so it never takes focus or shows a keyboard; the overlay
        // below handles the tap.
        TextField(
            value = date?.format(goalDateFormat).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Iconax.Calendar, contentDescription = null) },
            supportingText = if (isError && error != null) ({ Text(error) }) else null,
            shape = shape,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        )
    }
}

/** The "save X a month" hint: a primary-container wash, like the budget window row. */
@Composable
private fun GoalPerMonthBanner(text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(scheme.primaryContainer)
            .padding(horizontal = Dimensions.Padding.cardCompact, vertical = Spacing.smd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        Icon(
            Icons.Default.Savings,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(Dimensions.Icon.medium),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimaryContainer)
    }
}

/** Currency picker in the add-form field style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyField(
    currency: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ListItemPosition.Single.toShape(),
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        TextField(
            value = currency,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.goal_edit_currency), fontWeight = FontWeight.SemiBold) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = shape,
            colors = addFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
        ) {
            CurrencyFormatter.getSupportedCurrencies().forEach { code ->
                DropdownMenuItem(
                    text = { Text("${CurrencyFormatter.getCurrencySymbol(code)} $code") },
                    onClick = { onSelect(code); expanded = false },
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                )
            }
        }
    }
}
