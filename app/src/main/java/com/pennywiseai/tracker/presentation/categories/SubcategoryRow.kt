package com.pennywiseai.tracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.ui.effects.horizontalScrollFade
import com.pennywiseai.tracker.ui.icons.IconCatalog
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/** Tint strength for a chip's background — enough to read as its colour, light enough for text. */
private const val CHIP_BACKGROUND_ALPHA = 0.2f

/** Cashiro's visual size for the "+" tile. Its *touch* target is a full 48dp — see below. */
private val ADD_TILE_VISUAL_SIZE = Dimensions.Icon.large

/**
 * A category's subcategories as a horizontal chip strip, with a trailing "+".
 *
 * This is what makes subcategories visible at all in the categories list.
 */
@Composable
fun SubcategoryRow(
    subcategories: List<SubcategoryEntity>,
    onSubcategoryClick: (SubcategoryEntity) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = true,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScrollFade(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (subcategories.isEmpty()) {
            // Cashiro renders a row containing only the "+", which reads as a
            // stray control floating under the category. Say what it will add.
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.subcategory_none),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(subcategories, key = { it.id }) { subcategory ->
            SubcategoryChip(
                subcategory = subcategory,
                onClick = { onSubcategoryClick(subcategory) }
            )
        }

        if (showAddButton) {
            item(key = "add") {
                Box(
                    // The visual tile stays small, but the tappable area is a
                    // full touch target. Padding goes INSIDE the click surface;
                    // outside it and the padding is not tappable.
                    modifier = Modifier
                        .size(Dimensions.Component.minTouchTarget)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(ADD_TILE_VISUAL_SIZE)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.subcategory_add),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryChip(
    subcategory: SubcategoryEntity,
    onClick: () -> Unit,
) {
    val chipColor = parseColor(subcategory.color, MaterialTheme.colorScheme.primary)

    // Resolved once per icon name rather than on every recomposition of the row.
    val resId = remember(subcategory.iconName) {
        IconCatalog.all.firstOrNull { it.iconName == subcategory.iconName }?.resourceId ?: 0
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(chipColor.copy(alpha = CHIP_BACKGROUND_ALPHA))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            // One description for the whole chip; the icon inside is decorative.
            .semantics { contentDescription = subcategory.name },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (resId != 0) {
            Icon(
                painter = painterResource(resId),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(Dimensions.Icon.small)
            )
        }
        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
