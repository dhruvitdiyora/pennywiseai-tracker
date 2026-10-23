package com.pennywiseai.tracker.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.presentation.categories.CategoryAvatar
import com.pennywiseai.tracker.presentation.categories.CategorySearchField
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

internal fun filterCategoriesForQuery(
    categories: List<CategoryEntity>,
    query: String
): List<CategoryEntity> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        categories
    } else {
        categories.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
    }
}

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
    // Optional inline tagging (notification "More…" flow, #696). When
    // [onTagsChanged] is non-null a Tags section is shown above the category
    // list; each add/remove commits immediately, so the user can tag without
    // also picking a category. In-app call sites omit these and see no change.
    tagSuggestions: List<String> = emptyList(),
    initialTags: List<String> = emptyList(),
    onTagsChanged: ((List<String>) -> Unit)? = null,
    title: String? = null,
    searchPlaceholder: String? = null,
    // "Add category" on top of the list (#854); null hides it.
    onAddCategory: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val resolvedTitle = title ?: stringResource(R.string.quick_category_title)
    val resolvedSearchPlaceholder = searchPlaceholder
        ?: stringResource(R.string.category_picker_search_placeholder)
    val filteredCategories = remember(categories, searchQuery) {
        filterCategoriesForQuery(categories, searchQuery)
    }

    PennyWiseBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (onTagsChanged != null) {
            var tags by remember(initialTags) { mutableStateOf(initialTags) }
            TagInputField(
                selectedTags = tags,
                allTags = tagSuggestions,
                onAddTag = { name ->
                    if (name !in tags) {
                        tags = tags + name
                        onTagsChanged(tags)
                    }
                },
                onRemoveTag = { name ->
                    tags = tags - name
                    onTagsChanged(tags)
                },
                modifier = Modifier.padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Spacing.sm
                )
            )
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = Dimensions.Padding.dialog,
                    vertical = Spacing.xs
                )
            )
        }
        // Cashiro's picker, in the account sheet's family: a headline, the
        // pill search field from Categories, then grouped tonal rows with each
        // category's tinted avatar.
        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = Dimensions.Padding.dialog,
                end = Dimensions.Padding.dialog,
                bottom = Spacing.md
            )
        )
        CategorySearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" },
            placeholder = resolvedSearchPlaceholder,
            modifier = Modifier.padding(horizontal = Dimensions.Padding.dialog)
        )
        // LazyColumn so long category lists stay reachable when the sheet is
        // fully expanded; a plain Column would render items past the screen
        // bottom unscrollable.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.dialog,
                end = Dimensions.Padding.dialog,
                top = Spacing.md,
                bottom = Dimensions.Padding.dialog
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap)
        ) {
            if (onAddCategory != null) {
                item(key = "add-category") {
                    GroupedRow(
                        position = ListItemPosition.Single,
                        onClick = onAddCategory,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    ) {
                        IconTile(
                            icon = Icons.Default.Add,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = Dimensions.Icon.avatar
                        )
                        Text(
                            text = stringResource(R.string.txn_detail_add_category),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            itemsIndexed(filteredCategories, key = { _, category -> category.id }) { index, category ->
                CategoryPickerRow(
                    category = category,
                    selected = currentCategory == category.name,
                    position = ListItemPosition.from(index, filteredCategories.size),
                    onClick = { onCategorySelected(category.name) }
                )
            }
            if (filteredCategories.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (searchQuery.isBlank()) {
                                R.string.category_picker_empty
                            } else {
                                R.string.category_picker_no_results
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.lg)
                    )
                }
            }
        }
    }
}

/**
 * One category in the picker: its tinted avatar and name on a grouped tonal
 * row. The current category takes the selected (primary container) fill and
 * a check, as in the account sheet. Sub-categories indent under their parent
 * (#374).
 */
@Composable
private fun CategoryPickerRow(
    category: CategoryEntity,
    selected: Boolean,
    position: ListItemPosition,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    GroupedRow(
        position = position,
        onClick = onClick,
        containerColor = if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        contentPadding = PaddingValues(
            start = Spacing.md + if (category.parentId != null) Spacing.lg else Spacing.none,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.sm
        ),
        modifier = Modifier.semantics { this.selected = selected }
    ) {
        CategoryAvatar(category = category, size = Dimensions.Icon.avatar)
        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor
            )
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
    onDismiss: () -> Unit
) = QuickCategoryPickerSheet(
    currentCategory = transaction.category,
    categories = categories,
    onCategorySelected = onCategorySelected,
    onDismiss = onDismiss
)
