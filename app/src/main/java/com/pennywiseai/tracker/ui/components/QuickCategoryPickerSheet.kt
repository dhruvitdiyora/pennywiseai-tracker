package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * Modal bottom-sheet picker for changing a transaction's category. Used both
 * from the transactions list (long-press / overflow) and from the txn-alert
 * notification's "More categories…" action (#303). Shared so the in-app and
 * notification flows present identical UI.
 *
 * @param currentCategory The transaction's current category name — gets a
 *  check-mark and bold weight so the user sees what they're overriding.
 * @param categories Flat, already-ordered list of selectable categories.
 * @param onCategorySelected Fired with the chosen category name; caller is
 *  responsible for persisting and dismissing.
 * @param onDismiss Called when the user dismisses the sheet without picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategoryPickerSheet(
    currentCategory: String,
    categories: List<CategoryEntity>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    currentSubcategory: String? = null,
    subcategoriesByCategory: Map<Long, List<SubcategoryEntity>> = emptyMap(),
    onSelected: ((category: String, subcategory: String?) -> Unit)? = null,
    /**
     * When false a category selects immediately and subcategories are never
     * offered.
     *
     * Passed `false` by the notification picker: that flow exists so someone can
     * categorise one transaction in as few taps as possible from outside the
     * app, and adding a drill-in level there is a regression in the one place
     * speed matters most.
     */
    allowSubcategoryDrillIn: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Drill-in rather than an inline accordion: the sheet is height-constrained,
    // and expanding a category in place pushes everything after it below the fold.
    var drilledInto by remember { mutableStateOf<CategoryEntity?>(null) }

    fun emit(category: String, subcategory: String?) {
        onSelected?.invoke(category, subcategory) ?: onCategorySelected(category)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = stringResource(R.string.quick_category_picker_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                bottom = Spacing.sm
            )
        )
        // LazyColumn so long category lists stay reachable when the sheet is
        // fully expanded — a plain Column would render items past the screen
        // bottom unscrollable.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md)
        ) {
            val parent = drilledInto
            if (parent == null) {
                items(categories, key = { it.id }) { category ->
                    val subs = subcategoriesByCategory[category.id].orEmpty()
                    val canDrillIn = allowSubcategoryDrillIn && subs.isNotEmpty()
                    val selected = currentCategory == category.name

                    PickerRow(
                        label = category.name,
                        selected = selected,
                        leading = { CategoryChip(category = category, showText = false) },
                        // A category with no subcategories selects immediately —
                        // making everyone drill through a one-item level would be
                        // a tax on the common case.
                        onClick = {
                            if (canDrillIn) drilledInto = category
                            else emit(category.name, null)
                        },
                        trailing = {
                            if (canDrillIn) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            } else {
                item(key = "back") {
                    PickerRow(
                        label = stringResource(R.string.back),
                        selected = false,
                        leading = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { drilledInto = null }
                    )
                }
                item(key = "parent_only") {
                    // Keeps the parent selectable in one tap once drilled in —
                    // otherwise entering a category to look at its subcategories
                    // would trap you into choosing one.
                    PickerRow(
                        label = stringResource(R.string.category_just_parent, parent.name),
                        selected = currentCategory == parent.name && currentSubcategory == null,
                        leading = { CategoryChip(category = parent, showText = false) },
                        onClick = { emit(parent.name, null) }
                    )
                }
                items(
                    subcategoriesByCategory[parent.id].orEmpty(),
                    key = { it.id }
                ) { sub ->
                    PickerRow(
                        label = sub.name,
                        selected = currentCategory == parent.name && currentSubcategory == sub.name,
                        onClick = { emit(parent.name, sub.name) }
                    )
                }
            }
        }
    }
}

/**
 * Convenience overload for call sites that already have a TransactionEntity
 * in hand (e.g. TransactionsScreen). Delegates to the form above using the
 * transaction's current category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategoryPickerSheet(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    subcategoriesByCategory: Map<Long, List<SubcategoryEntity>> = emptyMap(),
    onSelected: ((category: String, subcategory: String?) -> Unit)? = null,
) = QuickCategoryPickerSheet(
    currentCategory = transaction.category,
    currentSubcategory = transaction.subcategory,
    categories = categories,
    subcategoriesByCategory = subcategoriesByCategory,
    onCategorySelected = onCategorySelected,
    onSelected = onSelected,
    onDismiss = onDismiss
)

/** One row of either level, so the two stay visually identical. */
@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimensions.Padding.content,
                vertical = Spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        leading?.invoke()
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        trailing?.invoke()
    }
}
