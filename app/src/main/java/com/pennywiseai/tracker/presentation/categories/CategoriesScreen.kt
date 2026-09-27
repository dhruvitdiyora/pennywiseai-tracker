package com.pennywiseai.tracker.presentation.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.repository.CategoryDeletionImpact
import com.pennywiseai.tracker.ui.components.CategoryChip
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.QuickCategoryPickerSheet
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
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
    
    // Group categories by type
    val expenseCategories = filteredCategories.filter { !it.isIncome }
    val incomeCategories = filteredCategories.filter { it.isIncome }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.categories_back))
                    }
                },
                hazeState = hazeState
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.categories_add))
            }
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()
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

            // Expense Categories Section
            if (expenseCategories.isNotEmpty()) {
                item {
                    SectionHeaderV2(title = stringResource(R.string.categories_expense_section))
                }

                items(
                    items = expenseCategories,
                    key = { it.id }
                ) { category ->
                    // Sub-categories sit indented under their parent (#374).
                    Box(modifier = Modifier.padding(start = if (category.parentId != null) Spacing.lg else Spacing.none)) {
                        SwipeableCategoryItem(
                            category = category,
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.requestCategoryDeletion(category) },
                            onToggleHidden = { viewModel.toggleCategoryHidden(category.id) }
                        )
                    }
                }
            }

            // Income Categories Section
            if (incomeCategories.isNotEmpty()) {
                item {
                    SectionHeaderV2(title = stringResource(R.string.categories_income_section))
                }

                items(
                    items = incomeCategories,
                    key = { it.id }
                ) { category ->
                    Box(modifier = Modifier.padding(start = if (category.parentId != null) Spacing.lg else Spacing.none)) {
                        SwipeableCategoryItem(
                            category = category,
                            onEdit = { viewModel.showEditDialog(category) },
                            onDelete = { viewModel.requestCategoryDeletion(category) },
                            onToggleHidden = { viewModel.toggleCategoryHidden(category.id) }
                        )
                    }
                }
            }
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

@Composable
internal fun CategorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.category_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
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
        shape = MaterialTheme.shapes.large,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHidden: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!category.isSystem) {
                        onDelete()
                        // Keep the row in place while impact is reviewed. The
                        // repository-backed list removes it only after the
                        // confirmed atomic operation succeeds.
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
                        .background(color)
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
            CategoryItem(
                category = category,
                onClick = if (!category.isSystem) onEdit else null,
                onToggleHidden = onToggleHidden
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !category.isSystem
    )
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    onClick: (() -> Unit)?,
    onToggleHidden: () -> Unit = {}
) {
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = Spacing.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
                // Dim a hidden category so it reads as "tucked away" in the manager.
                .alpha(if (category.isHidden) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category with colored dot
            CategoryChip(
                category = category,
                showText = true,
                modifier = Modifier.weight(1f)
            )

            // Hide / unhide toggle — available for every category, including
            // system defaults (the whole point of #736).
            IconButton(onClick = onToggleHidden) {
                Icon(
                    imageVector = if (category.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (category.isHidden) R.string.categories_show_category else R.string.categories_hide_category
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }

            // System badge
            if (category.isSystem) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.categories_system_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(
                            horizontal = Spacing.sm,
                            vertical = Spacing.xs
                        )
                    )
                }
            } else {
                // Edit icon for non-system categories
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.categories_action_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(Dimensions.Icon.medium)
                        .padding(start = Spacing.sm)
                )
            }
        }
    }
}
