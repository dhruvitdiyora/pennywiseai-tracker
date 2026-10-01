package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.icons.iconax.CloseCircle
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Search
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Small pieces shared by the Contacts, Person detail and Lend & Borrow screens,
 * so the three read as one family (Cashiro-style tonal chrome) instead of each
 * re-declaring its own avatar, FAB and field styling.
 */

/** Up to two letters naming a person: first + last initial, or the first two letters of a single name. */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

internal fun PersonEntity.initials(): String = initialsOf(name)

/**
 * Text colour that reads on a solid, user-chosen [color] (a contact's swatch).
 * Chosen by luminance, because a contact colour is arbitrary and no theme role
 * is guaranteed to contrast with it.
 */
internal fun contentColorOn(color: Color): Color =
    if (color.luminance() > CONTENT_LUMINANCE_THRESHOLD) Color.Black else Color.White

private const val CONTENT_LUMINANCE_THRESHOLD = 0.5f

/** The avatar that heads a person or loan detail screen: a step above a row avatar. */
internal val PersonHeaderAvatarSize: Dp = Dimensions.Icon.avatarLarge + Spacing.lg

/**
 * A circular initials avatar. [tinted] washes [color] into a light container
 * with the initials in a legible shade of the same hue (used where the colour
 * means a loan direction); otherwise [color] fills the circle (a contact's own
 * swatch).
 */
@Composable
internal fun PersonAvatar(
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.Icon.avatar,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    tinted: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (tinted) color.copy(alpha = Dimensions.Alpha.tonalIconContainer) else color
    val content = if (tinted) {
        color.legibleOn(
            background = container.compositeOver(scheme.surfaceContainerLow),
            towards = scheme.onSurface,
        )
    } else {
        contentColorOn(color)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
    }
}

/**
 * The round, tonal action button used in the trailing slot of the large top
 * bar, matching `TonalNavigationButton` on the leading side. [endPadding]
 * defaults to the screen gutter; pass a smaller value to the first of two
 * adjacent buttons.
 */
@Composable
internal fun PeopleTonalActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    endPadding: Dp = Dimensions.Padding.content,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(end = endPadding),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimensions.Icon.inline),
        )
    }
}

/**
 * The "+" action: a tonal extended FAB that keeps its [label] while the list is
 * at the top ([expanded]) and collapses to the glyph once it scrolls.
 */
@Composable
internal fun PeopleExtendedFab(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        expanded = expanded,
        icon = {
            // While the label is showing it already names the action.
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) null else label,
            )
        },
        text = { Text(label) },
        shape = if (expanded) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** Borderless fields on the card surface: the fill is the only container. */
@Composable
internal fun tonalTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerLow,
        unfocusedContainerColor = scheme.surfaceContainerLow,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerLow,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        disabledLabelColor = scheme.onSurfaceVariant,
        focusedPlaceholderColor = scheme.onSurfaceVariant,
        unfocusedPlaceholderColor = scheme.onSurfaceVariant,
        disabledTextColor = scheme.onSurface.copy(alpha = Dimensions.Alpha.medium),
        disabledTrailingIconColor = scheme.onSurfaceVariant,
    )
}

/**
 * A rounded, borderless field. [position] gives stacked fields the grouped-list
 * corner treatment (outer corners round, the seam between two fields nearly
 * square); the default is a single, fully rounded field.
 */
@Composable
internal fun TonalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    position: ListItemPosition = ListItemPosition.Single,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    prefix: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        prefix = prefix,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { message -> { Text(message) } },
        isError = isError,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = position.toShape(),
        colors = tonalTextFieldColors(),
    )
}

/**
 * The fully rounded search pill used by the Contacts list, matching the one on
 * the Categories screen: a borderless field on the card surface.
 */
@Composable
internal fun PeopleSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(imageVector = Iconax.Search, contentDescription = null)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Iconax.CloseCircle,
                        contentDescription = clearDescription,
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
        colors = tonalTextFieldColors(),
    )
}
