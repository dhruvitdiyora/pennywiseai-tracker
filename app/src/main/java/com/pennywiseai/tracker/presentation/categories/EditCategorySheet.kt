package com.pennywiseai.tracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.IconSelector
import com.pennywiseai.tracker.ui.components.isLightColor
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.ui.icons.IconCatalog
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/** Description is a caption, not an essay — long ones truncate everywhere they render. */
private const val DESCRIPTION_MAX_LENGTH = 60

/**
 * Create or edit a category: name, icon, colour, description, income/expense.
 *
 * Replaces `CategoryEditDialog`, which could not edit **system** categories at
 * all — and 16 of the 18 seeded categories are system ones, so almost nothing in
 * the list was editable. Doc 10's `default*` columns are what make lifting that
 * safe: [onResetToDefault] can always put a built-in category back.
 *
 * Deleting a system category stays blocked. Editing is reversible; deleting is
 * not.
 *
 * @param category null to create.
 * @param onDelete null hides the action — pass null for system categories.
 * @param onResetToDefault null hides the action — pass null when
 *   `category.defaultName == null` (a user-created category has no original).
 * @param nameError inline error from the caller, e.g. a name collision the
 *   repository refused. Shown on the field rather than as a snackbar after the
 *   sheet closes, so the user's input survives to be corrected.
 * @param defaultIsIncome initial type when creating.
 * @param lockType hides the income/expense toggle entirely — the caller already
 *   knows the type. Used by the transaction detail screen's inline create.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategorySheet(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, color: String, description: String, isIncome: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onResetToDefault: (() -> Unit)? = null,
    nameError: String? = null,
    /** null = still counting; the type toggle stays locked until it resolves. */
    hasTransactions: Boolean? = false,
    defaultIsIncome: Boolean = false,
    lockType: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var iconName by remember(category) { mutableStateOf(category?.iconName ?: "") }
    var color by remember(category) { mutableStateOf(category?.color ?: presetDefaultColor) }
    var description by remember(category) { mutableStateOf(category?.description ?: "") }
    var isIncome by remember(category) { mutableStateOf(category?.isIncome ?: defaultIsIncome) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // The caller's error refers to the name it was given. Once the user types
    // something else it is stale, so it stops showing AND stops blocking Save —
    // otherwise a rejected duplicate leaves the sheet permanently unsaveable.
    var erroredName by remember(nameError) { mutableStateOf(name) }
    val liveNameError = nameError?.takeIf { name == erroredName }

    val isValid = name.isNotBlank() && liveNameError == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ── Live preview ────────────────────────────────────────────────
            // Matches how the category will actually look in a transaction row,
            // so picking an icon and colour is grounded in the result rather
            // than in an abstract swatch.
            CategoryPreview(
                name = name,
                iconName = iconName,
                colorHex = color,
                description = description,
                isIncome = isIncome
            )

            // ── Icon ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { showIconPicker = true }) {
                    Text(
                        if (iconName.isEmpty()) stringResource(R.string.category_choose_icon)
                        else stringResource(R.string.category_change_icon)
                    )
                }
                if (iconName.isNotEmpty()) {
                    Text(
                        text = IconCatalog.all.firstOrNull { it.iconName == iconName }?.name
                            ?: stringResource(R.string.icon_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Name ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                isError = liveNameError != null,
                supportingText = liveNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Description ─────────────────────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= DESCRIPTION_MAX_LENGTH) description = it },
                label = { Text(stringResource(R.string.category_description_label)) },
                singleLine = true,
                supportingText = {
                    Text("${description.length}/$DESCRIPTION_MAX_LENGTH")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Colour ──────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.category_color_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            ColorPickerContent(
                selectedColor = color,
                onColorChanged = { color = it }
            )

            // ── Income / expense ────────────────────────────────────────────
            // Hidden entirely when locked, rather than shown disabled: the caller
            // already knows the type and a greyed control invites a pointless tap.
            if (!lockType) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        enabled = hasTransactions == false,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.expense)) }
                    SegmentedButton(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        enabled = hasTransactions == false,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.income)) }
                }
                if (hasTransactions == true) {
                    // Flipping this would silently move historical spend between
                    // the income and expense side of every chart. Cashiro allows
                    // it; we say why we don't.
                    Text(
                        text = stringResource(R.string.category_type_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Actions ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        // A name that no longer resolves would render the fallback
                        // glyph forever; store nothing rather than a broken name.
                        // Validated against the catalogue rather than the resource
                        // table: the catalogue is the set of icons this picker can
                        // actually offer, and it needs no Context.
                        //
                        // An unresolvable *non-empty* name (an import, or an asset
                        // dropped from the catalogue) falls back to the default
                        // icon — NOT to "". Empty means the user deliberately chose
                        // no icon, and silently turning a broken name into that
                        // choice would misreport what they picked.
                        val safeIcon = when {
                            iconName.isEmpty() -> ""
                            IconCatalog.all.any { it.iconName == iconName } -> iconName
                            else -> IconCatalog.default.iconName
                        }
                        onSave(name.trim(), safeIcon, color, description.trim(), isIncome)
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            if (onResetToDefault != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    onResetToDefault?.let {
                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(Modifier.size(Spacing.xs))
                            Text(stringResource(R.string.category_reset_to_default))
                        }
                    }
                    onDelete?.let {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.size(Spacing.xs))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }

    if (showIconPicker) {
        ModalBottomSheet(
            onDismissRequest = { showIconPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            IconSelector(
                selectedIconName = iconName,
                onIconSelected = {
                    iconName = it
                    showIconPicker = false
                },
                modifier = Modifier.fillMaxHeight(0.9f)
            )
        }
    }

    if (showResetConfirm && onResetToDefault != null) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.category_reset_to_default)) },
            // The second sentence is the one that matters: users reasonably fear a
            // reset will recategorise their history. It only touches this row.
            text = {
                Text(
                    stringResource(
                        R.string.category_reset_confirm,
                        category?.defaultName ?: name
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetToDefault()
                }) { Text(stringResource(R.string.category_reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.category_delete_confirm, name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** First preset, used as the starting colour for a new category. */
private val presetDefaultColor get() = "#43A047"

@Composable
private fun CategoryPreview(
    name: String,
    iconName: String,
    colorHex: String,
    description: String,
    isIncome: Boolean,
) {
    val color = parseColor(colorHex, MaterialTheme.colorScheme.primary)
    val resId = IconCatalog.all.firstOrNull { it.iconName == iconName }?.resourceId ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.list)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Icon(
                    painter = painterResource(resId),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
            } else {
                Text(
                    text = name.take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLightColor(color)) {
                        androidx.compose.ui.graphics.Color.Black
                    } else {
                        androidx.compose.ui.graphics.Color.White
                    }
                )
            }
        }
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name.ifBlank { stringResource(R.string.category_name_placeholder) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // The type is an editable field, so the preview has to show it —
                // otherwise flipping the toggle gives no feedback at all.
                Text(
                    text = if (isIncome) stringResource(R.string.income)
                    else stringResource(R.string.expense),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
