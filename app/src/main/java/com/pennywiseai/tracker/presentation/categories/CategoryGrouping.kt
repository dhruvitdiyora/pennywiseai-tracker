package com.pennywiseai.tracker.presentation.categories

import com.pennywiseai.tracker.data.database.entity.CategoryEntity

/**
 * A top-level category with the sub-categories (#374) that sit under it, as
 * the Categories screen shows them: one card per parent, its children as chips.
 */
internal data class CategoryGroup(
    val parent: CategoryEntity,
    val children: List<CategoryEntity>,
)

/**
 * Folds the flat category list into [CategoryGroup]s, keeping the list's own
 * order (parents in their order, each parent's children in theirs).
 *
 * [matched] is the search result for the current query; with a blank query it
 * is the whole list. A parent that matches shows all of its children, as a
 * match on "Food" should not hide "Coffee". A parent that does not match is
 * still shown when one of its children does, with only the matching children,
 * so a hit on a sub-category is never orphaned from the card that owns it. A
 * child whose parent is missing from [all] is promoted to a top-level card
 * rather than dropped.
 */
internal fun groupCategoriesForDisplay(
    all: List<CategoryEntity>,
    matched: List<CategoryEntity>,
): List<CategoryGroup> {
    val matchedIds = matched.mapTo(HashSet()) { it.id }
    val allIds = all.mapTo(HashSet()) { it.id }
    val childrenByParent = all
        .filter { it.parentId != null && it.parentId in allIds }
        .groupBy { it.parentId!! }

    return all
        .filter { it.parentId == null || it.parentId !in allIds }
        .mapNotNull { top ->
            val children = childrenByParent[top.id].orEmpty()
            val topMatches = top.id in matchedIds
            val shown = if (topMatches) children else children.filter { it.id in matchedIds }
            if (topMatches || shown.isNotEmpty()) CategoryGroup(top, shown) else null
        }
}
