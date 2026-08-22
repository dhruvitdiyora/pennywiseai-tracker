package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Currency switching for a caller-supplied anchor.
 *
 * The anchor keeps its own look — this owns only the behaviour, so the balance
 * card's chip and any other entry point can stay visually distinct without
 * each re-implementing the menu.
 *
 * **Two currencies tap straight through, three or more open a menu.** A menu
 * to choose between exactly two options is more work than the toggle it
 * replaces; past that, cycling means guessing how many taps get you back to
 * where you started, with no way to see what the options even are
 * (ui-revamp doc 47 step 2).
 *
 * @param anchor renders the tappable element. It is handed the click action to
 *   attach — do not attach your own, or the menu will not open.
 */
@Composable
fun CurrencyPickerMenu(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    anchor: @Composable (onClick: () -> Unit) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (availableCurrencies.size <= 2) {
        Box(modifier = modifier) {
            anchor {
                // Straight to the other one. `firstOrNull` rather than an index
                // dance so a malformed single-entry list is a no-op instead of
                // a crash.
                availableCurrencies
                    .firstOrNull { it != selectedCurrency }
                    ?.let(onCurrencySelected)
            }
        }
        return
    }

    Box(modifier = modifier) {
        anchor { menuExpanded = true }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = MaterialTheme.shapes.large
        ) {
            availableCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        // Marks the current one. Null description: the row's own
                        // text already names the currency, and "Selected, INR,
                        // INR" is worse than "INR".
                        if (currency == selectedCurrency) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}
