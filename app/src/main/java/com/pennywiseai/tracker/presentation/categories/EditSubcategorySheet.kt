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
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.ui.components.ColorPickerContent
import com.pennywiseai.tracker.ui.components.IconSelector
import com.pennywiseai.tracker.ui.components.isLightColor
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.ui.icons.IconCatalog
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * Create or edit one subcategory: name, icon, colour.
 *
 * Mirrors [EditCategorySheet] minus the income/expense toggle and description —
 * a subcategory inherits its parent's type by construction, and there is nowhere
 * a subcategory description would be shown.
 *
 * @param parentCategory required: a subcategory cannot exist without one, and
 *   the sheet shows it so "Coffee" is visibly landing under Food & Dining.
 * @param nameError inline error from the caller. Should name the parent —
 *   "already exists" alone is confusing when the same name is legal elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubcategorySheet(
    subcategory: SubcategoryEntity?,
    parentCategory: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, color: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onResetToDefault: (() -> Unit)? = null,
    nameError: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(subcategory) { mutableStateOf(subcategory?.name ?: "") }
    // Colour and icon default to the PARENT's on create. A subcategory that
    // visually belongs to its category is the entire point of the hierarchy;
    // Cashiro's grey default makes every new one look unrelated to its parent.
    var iconName by remember(subcategory) {
        mutableStateOf(subcategory?.iconName ?: parentCategory.iconName)
    }
    var color by remember(subcategory) {
        mutableStateOf(subcategory?.color ?: parentCategory.color)
    }

    var showIconPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var erroredName by remember(nameError) { mutableStateOf(name) }
    val liveNameError = nameError?.takeIf { name == erroredName }
    val isValid = name.isNotBlank() && liveNameError == null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            ParentHeader(parentCategory)

            SubcategoryPreview(name = name, iconName = iconName, colorHex = color)

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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                isError = liveNameError != null,
                supportingText = liveNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.category_color_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            ColorPickerContent(selectedColor = color, onColorChanged = { color = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val safeIcon = when {
                            iconName.isEmpty() -> ""
                            IconCatalog.all.any { it.iconName == iconName } -> iconName
                            else -> IconCatalog.default.iconName
                        }
                        onSave(name.trim(), safeIcon, color)
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
            text = {
                Text(
                    stringResource(
                        R.string.subcategory_reset_confirm,
                        subcategory?.defaultName ?: name
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
            // Transactions reference a subcategory by NAME, not by id, so deleting
            // one leaves those rows intact — they simply stop resolving and show
            // the category alone. Say so rather than letting the user assume the
            // worst.
            text = { Text(stringResource(R.string.subcategory_delete_confirm, name)) },
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

@Composable
private fun ParentHeader(parent: CategoryEntity) {
    val color = parseColor(parent.color, MaterialTheme.colorScheme.primary)
    val resId = remember(parent.iconName) {
        IconCatalog.all.firstOrNull { it.iconName == parent.iconName }?.resourceId ?: 0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.Icon.medium)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Icon(
                    painter = painterResource(resId),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(Dimensions.Icon.tiny)
                )
            }
        }
        Text(
            text = stringResource(R.string.subcategory_under_parent, parent.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubcategoryPreview(name: String, iconName: String, colorHex: String) {
    val color = parseColor(colorHex, MaterialTheme.colorScheme.primary)
    val resId = remember(iconName) {
        IconCatalog.all.firstOrNull { it.iconName == iconName }?.resourceId ?: 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
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
        Text(
            text = name.ifBlank { stringResource(R.string.subcategory_name_placeholder) },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
