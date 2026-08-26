package com.pennywiseai.tracker.presentation.categories

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SearchBarBox
import com.pennywiseai.tracker.ui.components.SearchBarPlaceholder
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.isLightColor
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.ui.icons.IconCatalog
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
    val visibleCategories by viewModel.visibleCategories.collectAsStateWithLifecycle()
    val subcategoriesByCategory by viewModel.subcategoriesByCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val migration by viewModel.migration.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilterMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbarMessage()
            }
        }
    }

    val expenseCategories = visibleCategories.filter { !it.isIncome }
    val incomeCategories = visibleCategories.filter { it.isIncome }

    // Two behaviours on purpose: CustomTitleTopAppBar renders a real
    // LargeTopAppBar whenever they differ, so there genuinely is something to
    // collapse. An inherited note calls this a bug; on this branch it is not.
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    // derivedStateOf, not snapshotFlow + LaunchedEffect: same meaning, but it
    // only recomposes when the boundary is actually crossed rather than
    // collecting forever.
    val fabExpanded by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex == 0 }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.categories_title),
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actionContent = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.categories_filter)
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            shape = MaterialTheme.shapes.large
                        ) {
                            CategoryTypeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label()) },
                                    onClick = {
                                        viewModel.setTypeFilter(filter)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (filter == typeFilter) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.categories_add)
                    )
                },
                text = { Text(stringResource(R.string.categories_add)) },
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
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Inside the list, not pinned above it: this screen IS a list, and a
            // permanently docked search field costs vertical space on every row.
            item(key = "search") {
                SearchBarBox(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    fieldLabel = stringResource(R.string.categories_search),
                    label = { SearchBarPlaceholder(stringResource(R.string.categories_search)) }
                )
            }

            if (visibleCategories.isEmpty()) {
                item(key = "empty") {
                    CategoriesEmptyState(
                        searchQuery = searchQuery,
                        typeFilter = typeFilter,
                        onClearSearch = { viewModel.setSearchQuery("") },
                        onAdd = { viewModel.showAddDialog() }
                    )
                }
            }

            if (expenseCategories.isNotEmpty()) {
                item(key = "header_expense") {
                    SectionHeaderV2(title = stringResource(R.string.categories_expense_section))
                }
                items(expenseCategories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        subcategories = subcategoriesByCategory[category.id].orEmpty(),
                        onEdit = { viewModel.showEditDialog(category) },
                        onDelete = { pendingDelete = category },
                        onSubcategoryClick = {
                            viewModel.showEditSubcategoryDialog(it, category)
                        },
                        onAddSubcategory = { viewModel.showAddSubcategoryDialog(category) }
                    )
                }
            }

            if (incomeCategories.isNotEmpty()) {
                item(key = "header_income") {
                    SectionHeaderV2(title = stringResource(R.string.categories_income_section))
                }
                items(incomeCategories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        subcategories = subcategoriesByCategory[category.id].orEmpty(),
                        onEdit = { viewModel.showEditDialog(category) },
                        onDelete = { pendingDelete = category },
                        onSubcategoryClick = {
                            viewModel.showEditSubcategoryDialog(it, category)
                        },
                        onAddSubcategory = { viewModel.showAddSubcategoryDialog(category) }
                    )
                }
            }
        }
    }

    // ── Sheets ──────────────────────────────────────────────────────────────
    // A `when` over one sealed value, so two sheets cannot both be open.
    when (val current = sheet) {
        is CategorySheet.None -> Unit

        is CategorySheet.EditCategory -> {
            val nameError by viewModel.nameError.collectAsStateWithLifecycle()
            val hasTransactions by viewModel.editingCategoryHasTransactions
                .collectAsStateWithLifecycle()

            EditCategorySheet(
                category = current.category,
                nameError = nameError,
                hasTransactions = hasTransactions,
                onDismiss = { viewModel.hideSheet() },
                onSave = { name, iconName, color, description, isIncome ->
                    viewModel.saveCategory(name, iconName, color, description, isIncome)
                },
                // Deleting a system category is blocked at the data layer; hiding
                // the action means the user is never offered something that will
                // be refused.
                onDelete = current.category?.takeIf { !it.isSystem }?.let { cat ->
                    { pendingDelete = cat }
                },
                onResetToDefault = current.category?.takeIf { it.defaultName != null }?.let { cat ->
                    { viewModel.resetCategoryToDefault(cat) }
                }
            )
        }

        is CategorySheet.EditSubcategory -> {
            val subcategoryNameError by viewModel.subcategoryNameError
                .collectAsStateWithLifecycle()

            EditSubcategorySheet(
                subcategory = current.subcategory,
                parentCategory = current.parent,
                nameError = subcategoryNameError,
                onDismiss = { viewModel.hideSheet() },
                onSave = { name, iconName, color ->
                    viewModel.saveSubcategory(name, iconName, color)
                },
                onDelete = current.subcategory?.let { sub ->
                    { viewModel.deleteSubcategory(sub) }
                },
                onResetToDefault = current.subcategory?.takeIf { it.defaultName != null }
                    ?.let { sub -> { viewModel.resetSubcategoryToDefault(sub) } }
            )
        }
    }

    pendingDelete?.let { category ->
        val subcategoryCount = subcategoriesByCategory[category.id].orEmpty().size
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.categories_delete_title, category.name)) },
            text = {
                Text(
                    // The cascade is invisible otherwise: deleting a category
                    // takes its subcategories with it (doc 11's FK), and a user
                    // who loses five of them without warning has no way back.
                    if (subcategoryCount > 0) {
                        pluralStringResource(
                            R.plurals.categories_delete_body_with_subs,
                            subcategoryCount,
                            subcategoryCount
                        )
                    } else {
                        stringResource(R.string.categories_delete_body)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    viewModel.hideSheet()
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    migration?.let { pending ->
        var target by remember(migration, categories) {
            mutableStateOf(categories.firstOrNull { it.id != pending.source.id })
        }
        AlertDialog(
            onDismissRequest = viewModel::hideMigrationSheet,
            title = { Text(stringResource(R.string.categories_migrate_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        stringResource(
                            R.string.categories_migrate_body,
                            pending.source.name,
                            pending.transactionCount
                        )
                    )
                    if (pending.transactionCount > 0) {
                        Text(stringResource(R.string.categories_migrate_to))
                        categories
                            .filter { it.id != pending.source.id }
                            .forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { target = category },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = target?.id == category.id,
                                        onClick = { target = category }
                                    )
                                    Text(category.name)
                                }
                            }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmMigrationToCategory(target) },
                    enabled = pending.transactionCount == 0 || target != null
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideMigrationSheet) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryTypeFilter.label(): String = when (this) {
    CategoryTypeFilter.ALL -> stringResource(R.string.categories_filter_all)
    CategoryTypeFilter.EXPENSE -> stringResource(R.string.expense)
    CategoryTypeFilter.INCOME -> stringResource(R.string.income)
}

/**
 * One category: icon, name, description, System badge, and its subcategory strip.
 *
 * Swipe actions are scoped to this header only. `SubcategoryRow` is a horizontal
 * `LazyRow` below it, so its horizontal scrolling and tap targets are not wrapped
 * by the swipe container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSubcategoryClick: (SubcategoryEntity) -> Unit,
    onAddSubcategory: () -> Unit,
) {
    val view = LocalView.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!category.isSystem) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onDelete()
                    }
                    false
                }
                else -> false
            }
        }
    )
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        // The list's verticalArrangement does not reach inside a single item, so
        // the internal rhythm is this card's own responsibility (trap 4).
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = !category.isSystem,
                backgroundContent = {
                    val backgroundColor by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                            else -> Color.Transparent
                        },
                        label = "category_swipe_background"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                            .padding(horizontal = Spacing.md),
                        horizontalArrangement = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                            else -> Arrangement.End
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                else -> Icons.Default.Delete
                            },
                            contentDescription = null,
                            tint = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> stringResource(R.string.categories_swipe_edit)
                                else -> stringResource(R.string.delete)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                },
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconTile(category)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (category.isSystem) {
                                    // Kept: it is the explanation for why Delete is absent.
                                    Text(
                                        text = stringResource(R.string.categories_system_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Omitted entirely when blank — an empty line reserved for
                            // a description most categories do not have makes every row taller.
                            if (category.description.isNotBlank()) {
                                Text(
                                    text = category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!category.isSystem) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )

            SubcategoryRow(
                subcategories = subcategories,
                onSubcategoryClick = onSubcategoryClick,
                onAddClick = onAddSubcategory
            )
        }
    }
}

@Composable
private fun CategoryIconTile(category: CategoryEntity) {
    val color = parseColor(category.color, MaterialTheme.colorScheme.primary)
    val resId = remember(category.iconName) {
        IconCatalog.all.firstOrNull { it.iconName == category.iconName }?.resourceId ?: 0
    }

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
                tint = Color.Unspecified,
                modifier = Modifier.size(Dimensions.Icon.small)
            )
        } else {
            // Falls back to the initial rather than a generic glyph, so two
            // icon-less categories are still distinguishable at a glance.
            Text(
                text = category.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isLightColor(color)) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun CategoriesEmptyState(
    searchQuery: String,
    typeFilter: CategoryTypeFilter,
    onClearSearch: () -> Unit,
    onAdd: () -> Unit,
) {
    when {
        searchQuery.isNotBlank() -> PennyWiseEmptyState(
            icon = Icons.Default.SearchOff,
            headline = stringResource(R.string.categories_empty_search_headline),
            description = stringResource(R.string.categories_empty_search_body, searchQuery),
            actionLabel = stringResource(R.string.clear_search),
            onAction = onClearSearch
        )

        typeFilter != CategoryTypeFilter.ALL -> PennyWiseEmptyState(
            icon = Icons.Default.FilterList,
            headline = stringResource(
                if (typeFilter == CategoryTypeFilter.INCOME) {
                    R.string.categories_empty_income_headline
                } else {
                    R.string.categories_empty_expense_headline
                }
            ),
            description = stringResource(R.string.categories_empty_filter_body),
            actionLabel = stringResource(R.string.categories_add),
            onAction = onAdd
        )

        else -> PennyWiseEmptyState(
            icon = Icons.Default.Category,
            headline = stringResource(R.string.categories_empty_headline),
            description = stringResource(R.string.categories_empty_body),
            actionLabel = stringResource(R.string.categories_add),
            onAction = onAdd
        )
    }
}
