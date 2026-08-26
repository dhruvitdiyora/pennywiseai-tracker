package com.pennywiseai.tracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.SubcategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val transactionDao: TransactionDao
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    // Categories list
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Which sheet is open, if any.
     *
     * One value rather than a boolean and a nullable per sheet: with four
     * independent flags the state "category sheet open AND subcategory sheet
     * open" is representable, and two `ModalBottomSheet`s fighting for the screen
     * is not a state anyone can recover from. Docs 14 and 15 deliberately left
     * their loose flows alone so this consolidation could cover both at once.
     */
    private val _sheet = MutableStateFlow<CategorySheet>(CategorySheet.None)
    val sheet: StateFlow<CategorySheet> = _sheet.asStateFlow()

    /** Convenience for the sheet's own state keying. */
    private val editingCategory: CategoryEntity?
        get() = (_sheet.value as? CategorySheet.EditCategory)?.category

    // Snackbar message
    // Search and filter live here, not in screen-local `remember`, so they
    // survive rotation and returning from a sheet.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow(CategoryTypeFilter.ALL)
    val typeFilter: StateFlow<CategoryTypeFilter> = _typeFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    /** Inline name-collision error for the edit sheet's name field. */
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    /**
     * Whether the category being edited already has transactions. Drives the
     * income/expense lock: flipping the type on a category with history would
     * silently move that spend between the income and expense side of every chart.
     */
    private val _editingCategoryHasTransactions = MutableStateFlow<Boolean?>(null)

    /**
     * null while the count is still running.
     *
     * Deliberately tri-state: defaulting to `false` opened a window where the
     * sheet rendered with the toggle *enabled* before the query returned, so a
     * fast user could flip the type of a category that does have history. The
     * sheet treats null as "locked until we know".
     */
    val editingCategoryHasTransactions: StateFlow<Boolean?> =
        _editingCategoryHasTransactions.asStateFlow()

    /** Cancels an in-flight count when the user opens a different category. */
    private var transactionCountJob: Job? = null

    /**
     * Every subcategory, grouped by parent id — **one** flow for the whole screen.
     *
     * Collecting `getSubcategoriesByCategoryId` inside each list item instead
     * would open ~18 database observers and re-subscribe them on every scroll.
     * That is the single most likely performance mistake in this feature.
     */
    val subcategoriesByCategory: StateFlow<Map<Long, List<SubcategoryEntity>>> =
        subcategoryRepository.getAllSubcategories()
            .map { all -> all.groupBy { it.categoryId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    private val _subcategoryNameError = MutableStateFlow<String?>(null)
    val subcategoryNameError: StateFlow<String?> = _subcategoryNameError.asStateFlow()

    private val _migration = MutableStateFlow<CategoryMigration?>(null)
    val migration: StateFlow<CategoryMigration?> = _migration.asStateFlow()

    fun showAddDialog() {
        _nameError.value = null
        transactionCountJob?.cancel()
        // A category that does not exist yet has no transactions, so this is
        // known immediately rather than pending.
        _editingCategoryHasTransactions.value = false
        _sheet.value = CategorySheet.EditCategory(null)
    }

    /**
     * System categories are editable now (ui-revamp doc 14). The old refusal was
     * a blunt instrument — 16 of the 18 seeded categories are system ones, so it
     * made almost nothing in this list editable. Doc 10's `default*` columns make
     * it safe: anything the user changes can be reset.
     *
     * Deleting a system category is still blocked; see [deleteCategory].
     */
    fun showEditDialog(category: CategoryEntity) {
        _nameError.value = null
        _editingCategoryHasTransactions.value = null
        // Cancelled and restarted per category, so a slow query for the category
        // the user just closed cannot land on the one they just opened.
        transactionCountJob?.cancel()
        transactionCountJob = viewModelScope.launch {
            _editingCategoryHasTransactions.value =
                transactionDao.getTransactionCountForCategory(category.name) > 0
        }
        _sheet.value = CategorySheet.EditCategory(category)
    }

    fun hideSheet() {
        _sheet.value = CategorySheet.None
        _nameError.value = null
        _subcategoryNameError.value = null
    }

    fun saveCategory(
        name: String,
        iconName: String,
        color: String,
        description: String,
        isIncome: Boolean
    ) {
        viewModelScope.launch {
            try {
                val editingCat = editingCategory

                if (editingCat != null) {
                    val updated = categoryRepository.updateCategory(
                        editingCat.copy(
                            name = name,
                            iconName = iconName,
                            color = color,
                            description = description,
                            isIncome = isIncome
                        )
                    )
                    if (!updated) {
                        // Inline on the field, not a snackbar after the sheet
                        // closes — the user's input has to survive to be fixed.
                        _nameError.value = "A category named \"$name\" already exists"
                        return@launch
                    }
                    _snackbarMessage.value = "Category updated"
                } else {
                    // One fully-populated insert. The previous shape created the
                    // row and then looked it up by name to add the icon — which,
                    // on a name collision, found and overwrote somebody else's
                    // category instead.
                    val createdId = categoryRepository.createCategory(
                        name = name,
                        color = color,
                        isIncome = isIncome,
                        iconName = iconName,
                        description = description
                    )
                    if (createdId == null) {
                        _nameError.value = "A category named \"$name\" already exists"
                        return@launch
                    }
                    _snackbarMessage.value = "Category created"
                }

                hideSheet()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving category: ${e.message}"
            }
        }
    }

    /**
     * Restores a system category's original name, icon, colour and description.
     * The sheet stays open afterwards so the user can see what came back.
     */
    fun resetCategoryToDefault(category: CategoryEntity) {
        viewModelScope.launch {
            val restored = categoryRepository.resetCategoryToDefault(category.id)
            if (restored) {
                // Re-seat the sheet with the restored row rather than closing it:
                // the user asked for their original back and should see it.
                _sheet.value = CategorySheet.EditCategory(
                    categoryRepository.getCategoryById(category.id)
                )
                _snackbarMessage.value = "Category reset to default"
            } else {
                _snackbarMessage.value = "Couldn't reset this category"
            }
        }
    }

    fun resetCategory(category: CategoryEntity) = resetCategoryToDefault(category)

    fun deleteCategory(category: CategoryEntity) {
        if (category.isSystem) {
            _snackbarMessage.value = "System categories cannot be deleted"
            return
        }

        viewModelScope.launch {
            try {
                showMigrationSheet(category)
            } catch (e: Exception) {
                _snackbarMessage.value = "Error deleting category: ${e.message}"
            }
        }
    }

    // ── Subcategories ───────────────────────────────────────────────────────
    //
    // These follow the ViewModel's current shape — a set of loose StateFlows.
    // Doc 16 step 7 replaces all of them, and the category ones, with a single
    // sealed sheet state; that consolidation needs to cover both sheets at once,
    // so it is deliberately not pre-empted here.

    fun showMigrationSheet(category: CategoryEntity) {
        if (category.isSystem) return
        viewModelScope.launch {
            val transactionCount = transactionDao.getTransactionCountForCategory(category.name)
            _migration.value = CategoryMigration(category, transactionCount)
        }
    }

    fun hideMigrationSheet() {
        _migration.value = null
    }

    fun confirmMigrationToCategory(target: CategoryEntity?) {
        val pending = _migration.value ?: return
        if (target == null && pending.transactionCount > 0) {
            _snackbarMessage.value = "Choose a category for migration"
            return
        }
        if (target?.id == pending.source.id) return

        viewModelScope.launch {
            try {
                target?.let { destination ->
                    transactionDao.getTransactionsByCategory(pending.source.name)
                        .first()
                        .forEach { transaction ->
                            transactionDao.updateCategoryAndSubcategory(
                                transactionId = transaction.id,
                                category = destination.name,
                                subcategory = null,
                                updatedAt = LocalDateTime.now()
                            )
                        }
                }
                val deleted = categoryRepository.deleteCategory(pending.source.id)
                _migration.value = null
                _snackbarMessage.value = if (deleted) {
                    "Category deleted successfully"
                } else {
                    "Cannot delete this category"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error deleting category: " + e.message
            }
        }
    }

    fun showAddSubcategoryDialog(parent: CategoryEntity) {
        _subcategoryNameError.value = null
        _sheet.value = CategorySheet.EditSubcategory(null, parent)
    }

    fun showEditSubcategoryDialog(subcategory: SubcategoryEntity, parent: CategoryEntity) {
        _subcategoryNameError.value = null
        _sheet.value = CategorySheet.EditSubcategory(subcategory, parent)
    }

    fun saveSubcategory(name: String, iconName: String, color: String) {
        val open = _sheet.value as? CategorySheet.EditSubcategory ?: return
        val parent = open.parent
        viewModelScope.launch {
            try {
                val editing = open.subcategory
                val ok = if (editing != null) {
                    subcategoryRepository.updateSubcategory(
                        editing.copy(name = name, iconName = iconName, color = color)
                    )
                } else {
                    subcategoryRepository.createSubcategory(
                        categoryId = parent.id,
                        name = name,
                        iconName = iconName,
                        color = color
                    ) != null
                }

                if (!ok) {
                    // Names are unique *within* a parent, so a bare "already
                    // exists" would be misleading — the same name is legal under
                    // a different category.
                    _subcategoryNameError.value = "\"$name\" already exists under ${parent.name}"
                    return@launch
                }
                hideSheet()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error saving subcategory: ${e.message}"
            }
        }
    }

    fun deleteSubcategory(subcategory: SubcategoryEntity) {
        viewModelScope.launch {
            subcategoryRepository.deleteSubcategory(subcategory.id)
            hideSheet()
        }
    }

    fun resetSubcategoryToDefault(subcategory: SubcategoryEntity) {
        viewModelScope.launch {
            val restored = subcategoryRepository.resetSubcategoryToDefault(subcategory.id)
            val open = _sheet.value as? CategorySheet.EditSubcategory
            if (restored && open != null) {
                _sheet.value = open.copy(
                    subcategory = subcategoryRepository.getSubcategoryById(subcategory.id)
                )
            } else if (!restored) {
                _snackbarMessage.value = "Couldn't reset this subcategory"
            }
        }
    }

    fun resetSubcategory(subcategory: SubcategoryEntity) =
        resetSubcategoryToDefault(subcategory)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: CategoryTypeFilter) {
        _typeFilter.value = filter
    }

    /**
     * Categories after search and type filtering.
     *
     * Search matches the category's name, its description **and its
     * subcategories' names** — so typing "coffee" surfaces Food & Dining.
     * Cashiro matches the category alone, which gets much less useful once a
     * taxonomy has two levels.
     */
    val visibleCategories: StateFlow<List<CategoryEntity>> = combine(
        categories,
        subcategoriesByCategory,
        _searchQuery,
        _typeFilter
    ) { all, subsByCategory, query, filter ->
        all.asSequence()
            .filter { category ->
                when (filter) {
                    CategoryTypeFilter.ALL -> true
                    CategoryTypeFilter.EXPENSE -> !category.isIncome
                    CategoryTypeFilter.INCOME -> category.isIncome
                }
            }
            .filter { category ->
                if (query.isBlank()) return@filter true
                category.name.contains(query, ignoreCase = true) ||
                    category.description.contains(query, ignoreCase = true) ||
                    subsByCategory[category.id].orEmpty()
                        .any { it.name.contains(query, ignoreCase = true) }
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

data class CategoryMigration(
    val source: CategoryEntity,
    val transactionCount: Int
)

/** Which of the two sheets this screen hosts is open. */
sealed interface CategorySheet {
    data object None : CategorySheet

    /** [category] null when creating. */
    data class EditCategory(val category: CategoryEntity?) : CategorySheet

    /** [subcategory] null when creating; [parent] is always known. */
    data class EditSubcategory(
        val subcategory: SubcategoryEntity?,
        val parent: CategoryEntity
    ) : CategorySheet
}

enum class CategoryTypeFilter { ALL, EXPENSE, INCOME }

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
