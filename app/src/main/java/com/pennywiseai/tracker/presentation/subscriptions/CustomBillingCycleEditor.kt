package com.pennywiseai.tracker.presentation.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.domain.model.SubscriptionCycleUnit
import com.pennywiseai.tracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomBillingCycleEditor(
    countInput: String,
    unit: SubscriptionCycleUnit,
    onCountChanged: (String) -> Unit,
    onUnitChanged: (SubscriptionCycleUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.subscription_cycle_interval_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TextField(
                value = countInput,
                onValueChange = { value ->
                    val digits = value.filter(Char::isDigit)
                    onCountChanged(digits)
                },
                label = { Text(stringResource(R.string.subscription_cycle_repeat_every)) },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.EventRepeat, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = countInput.toLongOrNull()?.let { it > 0 } != true,
                supportingText = if (countInput.toLongOrNull()?.let { it > 0 } != true) {
                    { Text(stringResource(R.string.subscription_cycle_count_error)) }
                } else null,
                modifier = Modifier.weight(1f),
            )
            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { unitMenuExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                TextField(
                    value = stringResource(unit.labelResource),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.subscription_cycle_unit)) },
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false },
                ) {
                    SubscriptionCycleUnit.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(stringResource(candidate.labelResource)) },
                            onClick = {
                                onUnitChanged(candidate)
                                unitMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
