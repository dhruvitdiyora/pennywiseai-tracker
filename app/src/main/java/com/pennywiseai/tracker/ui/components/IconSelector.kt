package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.icons.IconCatalog
import com.pennywiseai.tracker.ui.icons.IconItem
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * The category-icon picker: ~496 icons in 19 groups, searchable.
 *
 * Emits the **stable resource name** (`"type_food_pizza"`), never a resource id —
 * Android renumbers ids between builds, so an id in the database or a backup
 * eventually points at the wrong picture.
 *
 * This is sheet *content*, not a sheet. Callers host it in their own
 * `ModalBottomSheet`, which is what lets the category and subcategory sheets
 * frame it differently.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IconSelector(
    selectedIconName: String?,
    onIconSelected: (iconName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val allIcons = IconCatalog.all

    // 496 items is small enough that a plain filter beats an index. Keyed on the
    // query so it does not re-run on every unrelated recomposition.
    val filtered = remember(query, allIcons) {
        if (query.isBlank()) {
            allIcons
        } else {
            allIcons.filter { icon ->
                icon.name.contains(query, ignoreCase = true) ||
                    icon.category.contains(query, ignoreCase = true) ||
                    // Aliases exist so "Music" finds the headphone icon without
                    // the catalogue listing the same picture twice.
                    icon.aliases.any { it.contains(query, ignoreCase = true) }
            }
        }
    }

    val grouped = remember(filtered) { filtered.groupBy { it.category }.toList() }

    Column(modifier = modifier.fillMaxWidth()) {
        SearchBarBox(
            searchQuery = query,
            onSearchQueryChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            fieldLabel = stringResource(R.string.search_icons),
            label = { SearchBarPlaceholder(stringResource(R.string.search_icons)) }
        )

        if (grouped.isEmpty()) {
            PennyWiseEmptyState(
                icon = Icons.Default.SearchOff,
                headline = stringResource(R.string.icon_search_empty_headline),
                description = stringResource(R.string.icon_search_empty_description, query)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = Spacing.xl
            )
        ) {
            // The clear-selection slot leads the list rather than sitting inside a
            // group: Cashiro offers no way to un-pick an icon once picked, which is
            // a dead end. Rendered as an outlined empty slot so it does not read as
            // a 497th icon.
            item(key = "__clear__") {
                Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
                    IconTile(
                        item = null,
                        isSelected = selectedIconName.isNullOrEmpty(),
                        tileDescription = stringResource(R.string.icon_none),
                        onClick = { onIconSelected("") }
                    )
                    Text(
                        text = stringResource(R.string.icon_none),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xxs)
                    )
                }
            }

            grouped.forEach { (category, icons) ->
                stickyHeader(key = "header_$category") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = iconGroupLabel(category),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = Spacing.md,
                                vertical = Spacing.sm
                            )
                        )
                    }
                }

                item(key = "group_$category") {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        icons.forEach { icon ->
                            IconTile(
                                item = icon,
                                isSelected = icon.iconName == selectedIconName,
                                tileDescription = icon.name,
                                onClick = { onIconSelected(icon.iconName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One tile. [item] null renders the "no icon" slot.
 *
 * Selection is marked by a border **and** a shadow, not colour alone — colour on
 * its own is not a sufficient cue.
 */
@Composable
private fun IconTile(
    item: IconItem?,
    isSelected: Boolean,
    tileDescription: String,
    onClick: () -> Unit,
) {
    val border = if (isSelected) {
        BorderStroke(Dimensions.Component.dividerThickness * 2, MaterialTheme.colorScheme.primary)
    } else if (item == null) {
        BorderStroke(Dimensions.Component.hairline, MaterialTheme.colorScheme.outline)
    } else {
        null
    }

    Surface(
        onClick = onClick,
        // The glyph is smaller than this, but the target must not be.
        //
        // The label and selected state live here rather than on the inner Icon:
        // the "no icon" tile has no Icon to hang them on, and a screen reader
        // needs to know *which* tile is chosen, not just that one of them is.
        modifier = Modifier
            .semantics {
                contentDescription = tileDescription
                selected = isSelected
                role = Role.RadioButton
            }
            .size(Dimensions.Component.minTouchTarget)
            .then(
                if (isSelected) Modifier.shadow(Dimensions.Elevation.raisedCard, CircleShape)
                else Modifier
            ),
        shape = CircleShape,
        color = if (item == null) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = border
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (item != null) {
                Icon(
                    painter = painterResource(item.resourceId),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

/**
 * Maps [IconItem.category] — a stable English key — to a translatable heading.
 *
 * The keys stay in the catalogue and the translations stay in `strings.xml`;
 * storing a resource id in the catalogue would make it untestable on the JVM
 * again, which is the whole reason doc 01 kept `Context` out of it.
 *
 * An unknown key falls through to itself rather than crashing: a new group added
 * to the catalogue should show its English name until someone translates it, not
 * take the picker down.
 */
@Composable
private fun iconGroupLabel(category: String): String = when (category) {
    "Animals" -> stringResource(R.string.icon_group_animals)
    "Beverages" -> stringResource(R.string.icon_group_beverages)
    "Events & Places" -> stringResource(R.string.icon_group_events_places)
    "Finance" -> stringResource(R.string.icon_group_finance)
    "Flowers & Nature" -> stringResource(R.string.icon_group_flowers_nature)
    "Food" -> stringResource(R.string.icon_group_food)
    "Fruits" -> stringResource(R.string.icon_group_fruits)
    "Groceries" -> stringResource(R.string.icon_group_groceries)
    "Health" -> stringResource(R.string.icon_group_health)
    "Human" -> stringResource(R.string.icon_group_human)
    "Musical Instruments" -> stringResource(R.string.icon_group_musical_instruments)
    "Shopping" -> stringResource(R.string.icon_group_shopping)
    "Snacks" -> stringResource(R.string.icon_group_snacks)
    "Sports" -> stringResource(R.string.icon_group_sports)
    "Stationery" -> stringResource(R.string.icon_group_stationery)
    "Sweets" -> stringResource(R.string.icon_group_sweets)
    "Tools" -> stringResource(R.string.icon_group_tools)
    "Travel" -> stringResource(R.string.icon_group_travel)
    "Vegetables" -> stringResource(R.string.icon_group_vegetables)
    else -> category
}
