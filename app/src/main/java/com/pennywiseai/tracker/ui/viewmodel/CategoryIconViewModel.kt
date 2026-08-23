package com.pennywiseai.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.CategoryRepository
import com.pennywiseai.tracker.data.repository.SubcategoryRepository
import com.pennywiseai.tracker.ui.components.CategoryIconLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Supplies [CategoryIconLookup] for the whole app (ui-revamp doc 18).
 *
 * Its own ViewModel, hoisted to the NavHost, rather than a field on each screen's:
 * transaction rows appear on Home, the transactions list, account detail, group
 * detail and search, and every one of them needs the same two maps. Building them
 * once here beats duplicating the plumbing five times — and beats building them
 * per row, which would be O(rows) map constructions on the app's hottest list.
 */
@HiltViewModel
class CategoryIconViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    subcategoryRepository: SubcategoryRepository,
) : ViewModel() {

    val lookup: StateFlow<CategoryIconLookup> = combine(
        categoryRepository.getAllCategories(),
        subcategoryRepository.getAllSubcategories()
    ) { categories, subcategories ->
        val categoriesById = categories.associateBy { it.id }
        CategoryIconLookup(
            categoriesByName = categories.associateBy { it.name },
            // Keyed by (parent name, own name): a subcategory name is unique only
            // within its parent, so keying by name alone would hand rows under a
            // different category the wrong icon. A subcategory whose parent has
            // since vanished is dropped — it can no longer be addressed anyway.
            subcategoriesByName = subcategories.mapNotNull { sub ->
                val parentName = categoriesById[sub.categoryId]?.name ?: return@mapNotNull null
                (parentName to sub.name) to sub
            }.toMap()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryIconLookup()
    )
}
