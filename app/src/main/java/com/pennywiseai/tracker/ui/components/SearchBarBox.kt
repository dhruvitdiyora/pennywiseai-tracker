package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * The app's single in-list search field.
 *
 * Extracted from `TransactionsScreen`'s private `TransactionSearchBar` rather
 * than ported from Cashiro (ui-revamp doc 22): the existing implementation was
 * already token-clean and battle-tested, and one shared component beats a
 * ported second one.
 *
 * Deliberately **not** included:
 * - **No rotating placeholder.** Cashiro drives the hint from a `while (true)`
 *   loop in each caller, cycling every 3s. That is unbounded recomposition and
 *   unstoppable motion that conveys nothing. The [label] slot stays open so a
 *   caller *can* animate it if a real reason ever appears; no caller does.
 * - **No debounce.** This emits every change; debouncing is the caller's job.
 *   In-memory filters over a handful of items would only gain latency from it,
 *   and a caller filtering a database table can debounce in its ViewModel.
 *
 * @param searchQuery current value. [TextFieldValue] rather than [String] so
 *   callers keep cursor position — the clear action has to reset it.
 * @param fieldLabel accessible name for the field itself. A placeholder is not
 *   a label: it disappears the moment there is text, and TalkBack announces
 *   nothing without this.
 * @param label placeholder shown while the field is empty.
 * @param leadingIcon replaces the default magnifier.
 * @param trailingIcon **replaces** the built-in clear button. The clear button
 *   is built in rather than left to callers precisely because callers forget it.
 * @param trailingContent extra actions rendered *after* the clear button (a
 *   sort/overflow menu, say) — additive, unlike [trailingIcon].
 * @param onSearch invoked on the IME search action; the keyboard is dismissed
 *   either way.
 */
@Composable
fun SearchBarBox(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    fieldLabel: String = stringResource(R.string.search),
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onSearch: (() -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val textColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.height(Dimensions.Component.minTouchTarget),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Spacing.md, end = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            } else {
                // Decorative on purpose: the field itself is labelled
                // `fieldLabel`, so describing this too makes TalkBack announce
                // "Search, Search". The clear button DOES need a description —
                // it is an action, not decoration.
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }

            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearch?.invoke()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = fieldLabel }
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.text.isEmpty() && label != null) {
                            label()
                        }
                        innerTextField()
                    }
                }
            )

            if (trailingIcon != null) {
                trailingIcon()
            } else if (searchQuery.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        // Reset the cursor with the text; leaving a stale
                        // selection behind is what TextFieldValue exists to
                        // prevent.
                        onSearchQueryChange(TextFieldValue(""))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            trailingContent?.invoke()
        }
    }
}

/**
 * [String]-valued convenience overload for callers whose query lives in a
 * ViewModel as plain text.
 *
 * The cursor is UI state and has no business in a ViewModel, so the
 * [TextFieldValue] is held here instead: typing keeps the caret where the user
 * put it, and an external change (a programmatic clear, a restored filter)
 * moves it to the end rather than leaving it dangling past the new text.
 */
@Composable
fun SearchBarBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldLabel: String = stringResource(R.string.search),
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onSearch: (() -> Unit)? = null,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(searchQuery)) }
    if (fieldValue.text != searchQuery) {
        fieldValue = fieldValue.copy(
            text = searchQuery,
            selection = TextRange(searchQuery.length)
        )
    }

    SearchBarBox(
        searchQuery = fieldValue,
        onSearchQueryChange = { newValue ->
            val textChanged = newValue.text != fieldValue.text
            fieldValue = newValue
            if (textChanged) onSearchQueryChange(newValue.text)
        },
        modifier = modifier,
        fieldLabel = fieldLabel,
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        trailingContent = trailingContent,
        focusRequester = focusRequester,
        onSearch = onSearch,
    )
}

/**
 * The default placeholder rendering — a single ellipsised line in the muted
 * role. Passed as [SearchBarBox]'s `label` so callers don't each restyle it.
 */
@Composable
fun SearchBarPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
