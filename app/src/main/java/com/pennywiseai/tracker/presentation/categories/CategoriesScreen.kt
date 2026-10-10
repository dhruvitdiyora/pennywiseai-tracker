package com.pennywiseai.tracker.presentation.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.repository.CategoryDeletionImpact
import com.pennywiseai.tracker.ui.components.CategoryIcon
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.EmojiGlyph
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.TINTED_CONTAINER_ALPHA
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.components.toColorOr
import com.pennywiseai.tracker.ui.icons.iconax.CloseCircle
import com.pennywiseai.tracker.ui.icons.iconax.Eye
import com.pennywiseai.tracker.ui.icons.iconax.EyeSlash
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredCategories by viewModel.filteredCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val categoryDeletion by viewModel.categoryDeletion.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar messages
    val snackbarText = snackbarMessage?.asString()
    LaunchedEffect(snackbarMessage) {
        snackbarText?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbarMessage()
            }
        }
    }

    // One card per top-level category with its sub-categories (#374) as chips,
    // split into the expense and income sections.
    val groups = remember(categories, filteredCategories) {
        groupCategoriesForDisplay(categories, filteredCategories)
    }
    val expenseGroups = groups.filter { !it.parent.isIncome }
    val incomeGroups = groups.filter { it.parent.isIncome }
    val expenseTitle = stringResource(R.string.categories_expense_section)
    val incomeTitle = stringResource(R.string.categories_income_section)

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    // The add button carries its label only while the list is at the top.
    val fabExpanded by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.categories_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.categories_back)
                    )
                },
                hazeState = hazeState
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                expanded = fabExpanded,
                icon = {
                    // While the label is showing it already names the action.
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (fabExpanded) null else stringResource(R.string.categories_add)
                    )
                },
                text = { Text(stringResource(R.string.categories_add)) },
                shape = if (fabExpanded) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = Dimensions.Component.fabScrollClearance
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            item {
                CategorySearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClear = viewModel::clearSearchQuery,
                )
            }

            if (searchQuery.isNotBlank() && filteredCategories.isEmpty()) {
                item {
                    PennyWiseEmptyState(
                        icon = Icons.Default.Search,
                        headline = stringResource(R.string.category_search_no_results_title),
                        description = stringResource(R.string.category_search_no_results_description),
                    )
                }
            }

            categorySection(
                title = expenseTitle,
                groups = expenseGroups,
                onEdit = viewModel::showEditDialog,
                onDelete = viewModel::requestCategoryDeletion,
                onToggleHidden = { viewModel.toggleCategoryHidden(it.id) },
            )
            categorySection(
                title = incomeTitle,
                groups = incomeGroups,
                onEdit = viewModel::showEditDialog,
                onDelete = viewModel::requestCategoryDeletion,
                onToggleHidden = { viewModel.toggleCategoryHidden(it.id) },
            )
        }
    }

    // Add/Edit Dialog
    if (showAddEditDialog) {
        CategoryEditDialog(
            category = editingCategory,
            parentOptions = categories,
            onDismiss = { viewModel.hideDialog() },
            onSave = { name, color, isIncome, icon, parentId ->
                viewModel.saveCategory(name, color, isIncome, icon, parentId)
            },
            onDelete = editingCategory?.let { cat ->
                {
                    viewModel.hideDialog()
                    viewModel.requestCategoryDeletion(cat)
                }
            }
        )
    }

    val deletionCategory = categoryDeletion.category
    val deletionImpact = categoryDeletion.impact
    if (deletionCategory != null && !categoryDeletion.showReplacementPicker) {
        CategoryDeletionDialog(
            category = deletionCategory,
            impact = deletionImpact,
            isLoading = categoryDeletion.isLoading,
            isApplying = categoryDeletion.isApplying,
            onDismiss = viewModel::dismissCategoryDeletion,
            onDeleteUnused = { viewModel.confirmCategoryDeletion() },
            onChooseReplacement = viewModel::showReplacementPicker,
        )
    }

    if (deletionCategory != null && categoryDeletion.showReplacementPicker) {
        val replacementCategories = categories.filter {
            it.id != deletionCategory.id && it.isIncome == deletionCategory.isIncome
        }
        QuickCategoryPickerSheet(
            currentCategory = deletionCategory.name,
            categories = replacementCategories,
            title = stringResource(R.string.category_delete_replacement_title),
            searchPlaceholder = stringResource(R.string.category_delete_replacement_search),
            onCategorySelected = { selectedName ->
                replacementCategories.firstOrNull { it.name == selectedName }?.let {
                    viewModel.confirmCategoryDeletion(it)
                }
            },
            onDismiss = viewModel::hideReplacementPicker,
        )
    }
}

/** A titled run of category cards (the expense or the income section). */
private fun LazyListScope.categorySection(
    title: String,
    groups: List<CategoryGroup>,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onToggleHidden: (CategoryEntity) -> Unit,
) {
    if (groups.isEmpty()) return
    item(key = "section-$title") {
        SectionHeaderV2(title = title)
    }
    items(
        items = groups,
        key = { it.parent.id }
    ) { group ->
        SwipeableCategoryItem(
            group = group,
            onEdit = onEdit,
            onDelete = onDelete,
            onToggleHidden = onToggleHidden,
        )
    }
}

/**
 * Cashiro's search pill: a fully rounded, borderless field on the card
 * surface with a centred, italic prompt.
 */
@Composable
internal fun CategorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = stringResource(R.string.category_search_placeholder),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Iconax.CloseCircle,
                        contentDescription = stringResource(R.string.category_search_clear),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerLow,
            unfocusedContainerColor = scheme.surfaceContainerLow,
            disabledContainerColor = scheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = scheme.onSurfaceVariant,
            unfocusedPlaceholderColor = scheme.onSurfaceVariant,
        ),
    )
}

@Composable
internal fun CategoryDeletionDialog(
    category: CategoryEntity,
    impact: CategoryDeletionImpact?,
    isLoading: Boolean,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onDeleteUnused: () -> Unit,
    onChooseReplacement: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        iconContentColor = MaterialTheme.colorScheme.error,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = {
            Text(stringResource(R.string.category_delete_title, category.name))
        },
        text = {
            when {
                isLoading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(Dimensions.Icon.medium))
                    Text(stringResource(R.string.category_delete_checking))
                }

                impact == null -> Text(stringResource(R.string.category_delete_check_failed))

                !impact.hasReferences -> Text(stringResource(R.string.category_delete_unused_body))

                else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        stringResource(
                            R.string.category_delete_used_body,
                            impact.totalReferences,
                        )
                    )
                    CategoryDeletionImpactSummary(impact)
                    Text(
                        text = stringResource(R.string.category_delete_replacement_requirement),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            when {
                isLoading || impact == null -> Unit
                impact.hasReferences -> TextButton(
                    enabled = !isApplying,
                    onClick = onChooseReplacement,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.category_delete_choose_replacement))
                }

                else -> TextButton(
                    enabled = !isApplying,
                    onClick = onDeleteUnused,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.category_delete_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isApplying,
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.category_delete_cancel))
            }
        },
    )
}

@Composable
private fun CategoryDeletionImpactSummary(impact: CategoryDeletionImpact) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (impact.transactionCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_transactions,
                    impact.transactionCount,
                    impact.transactionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (impact.transactionSplitCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_transaction_splits,
                    impact.transactionSplitCount,
                    impact.transactionSplitCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (impact.subscriptionCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_subscriptions,
                    impact.subscriptionCount,
                    impact.subscriptionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (impact.recurringTransactionCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_recurring,
                    impact.recurringTransactionCount,
                    impact.recurringTransactionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (impact.merchantMappingCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_merchant_mappings,
                    impact.merchantMappingCount,
                    impact.merchantMappingCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val ruleCount = impact.ruleConditionCount + impact.ruleActionCount
        if (ruleCount > 0) {
            Text(
                pluralStringResource(R.plurals.category_delete_rules, ruleCount, ruleCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (impact.activeBudgetCategoryCount > 0) {
            Text(
                pluralStringResource(
                    R.plurals.category_delete_budgets,
                    impact.activeBudgetCategoryCount,
                    impact.activeBudgetCategoryCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * A category card that swipes away to start the delete flow. The swipe never
 * removes the row itself: the impact dialog decides, and the repository-backed
 * list drops it only after the confirmed atomic operation succeeds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    group: CategoryGroup,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onToggleHidden: (CategoryEntity) -> Unit,
) {
    val category = group.parent
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!category.isSystem) {
                        onDelete(category)
                        // Keep the row in place while impact is reviewed.
                        false
                    } else {
                        false // Don't allow swipe for system categories
                    }
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (!category.isSystem) {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    },
                    label = "background color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, shape = MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = Dimensions.Padding.content),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.categories_action_delete),
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        },
        content = {
            CategoryGroupCard(
                group = group,
                onEdit = onEdit,
                onToggleHidden = onToggleHidden,
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !category.isSystem
    )
}

/**
 * Cashiro's category card: a tinted rounded-square avatar, the name and a
 * quiet subtitle, with the sub-categories (#374) as a scrolling row of chips
 * underneath. The hide / unhide toggle (#736) stays on every card, system
 * defaults included.
 */
@Composable
private fun CategoryGroupCard(
    group: CategoryGroup,
    onEdit: (CategoryEntity) -> Unit,
    onToggleHidden: (CategoryEntity) -> Unit,
) {
    val category = group.parent
    val children = group.children
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = if (!category.isSystem) ({ onEdit(category) }) else null,
        contentPadding = Spacing.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Dimensions.Padding.content,
                    top = Dimensions.Padding.content,
                    end = Spacing.sm,
                    bottom = if (children.isEmpty()) Dimensions.Padding.content else Spacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    // Dim a hidden category so it reads as "tucked away" in the manager.
                    .alpha(if (category.isHidden) 0.5f else 1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryAvatar(category = category, size = Dimensions.Icon.avatarLarge)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (category.isSystem || children.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (category.isSystem) {
                                SubtitleTag(
                                    text = stringResource(R.string.categories_system_badge),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (children.isNotEmpty()) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.categories_subcategory_count,
                                        children.size,
                                        children.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Hide / unhide toggle — available for every category, including
            // system defaults (the whole point of #736).
            IconButton(onClick = { onToggleHidden(category) }) {
                Icon(
                    imageVector = if (category.isHidden) Iconax.EyeSlash else Iconax.Eye,
                    contentDescription = stringResource(
                        if (category.isHidden) R.string.categories_show_category else R.string.categories_hide_category
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.inline)
                )
            }
        }

        if (children.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    bottom = Dimensions.Padding.content
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(items = children, key = { it.id }) { child ->
                    SubCategoryChip(
                        category = child,
                        onEdit = if (!child.isSystem) ({ onEdit(child) }) else null,
                        onToggleHidden = { onToggleHidden(child) }
                    )
                }
            }
        }
    }
}

/**
 * A sub-category as a tinted pill. Tapping it opens a small menu: edit (user
 * categories only, which is also the way to delete one) and hide / unhide.
 */
@Composable
private fun SubCategoryChip(
    category: CategoryEntity,
    onEdit: (() -> Unit)?,
    onToggleHidden: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val tint = rememberLegibleCategoryTint(category)

    Box {
        Surface(
            onClick = { menuOpen = true },
            shape = CircleShape,
            color = tint.container,
            modifier = Modifier.alpha(if (category.isHidden) 0.5f else 1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.smd, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryGlyph(category = category, size = Dimensions.Icon.small, tint = tint.glyph)
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = MaterialTheme.shapes.large
        ) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.categories_action_edit)) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (category.isHidden) R.string.categories_show_category else R.string.categories_hide_category
                        )
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggleHidden()
                }
            )
        }
    }
}

/**
 * A category's colour as drawn on the card: the container wash and the glyph
 * on top of it. Category colours are swatches, not text colours — the default
 * black "Transportation" vanishes on a dark card and a pale user colour on a
 * light one — so the colour is first nudged to read against the card, and the
 * glyph again against its own wash.
 */
private class CategoryTint(val container: Color, val glyph: Color)

@Composable
private fun rememberLegibleCategoryTint(category: CategoryEntity): CategoryTint {
    val scheme = MaterialTheme.colorScheme
    val card = scheme.surfaceContainerLow
    val onSurface = scheme.onSurface
    val raw = category.color.toColorOr(scheme.primary)
    return remember(raw, card, onSurface) {
        val legible = raw.legibleOn(card, towards = onSurface)
        val container = legible.copy(alpha = TINTED_CONTAINER_ALPHA)
        CategoryTint(
            container = container,
            glyph = legible.legibleOn(container.compositeOver(card), towards = onSurface),
        )
    }
}

/** The category's glyph in a rounded square of its own colour, as in Cashiro. */
@Composable
private fun CategoryAvatar(
    category: CategoryEntity,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val tint = rememberLegibleCategoryTint(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.large)
            .background(tint.container),
        contentAlignment = Alignment.Center
    ) {
        CategoryGlyph(category = category, size = Dimensions.Icon.medium, tint = tint.glyph)
    }
}

/**
 * The user's emoji (#760) when one is set, else the category's built-in icon
 * in its own colour. Emoji can't be tinted, so the surrounding container
 * carries the colour.
 */
@Composable
private fun CategoryGlyph(
    category: CategoryEntity,
    size: Dp,
    tint: Color,
) {
    val emoji = category.icon?.takeIf { it.isNotBlank() }
    if (emoji != null) {
        EmojiGlyph(emoji = emoji, box = size)
    } else {
        CategoryIcon(category = category.name, size = size, tint = tint)
    }
}
