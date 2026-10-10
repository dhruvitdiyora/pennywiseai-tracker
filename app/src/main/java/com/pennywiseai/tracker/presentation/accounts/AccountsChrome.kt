package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.domain.model.getAccountType
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.WalletMoney
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

/*
 * Small pieces shared by the Manage Accounts list, its editor sheets and the
 * Add Account screen, so the accounts screens read as one Cashiro-style family
 * instead of each re-declaring its own avatar, menu and tonal field.
 */

/**
 * The avatar of an account: the bank's logo, or a wallet glyph for a manually
 * tracked cash account (which has no bank to show).
 */
@Composable
internal fun AccountAvatar(
    account: AccountBalanceEntity,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.Icon.avatarLarge,
) {
    AccountAvatar(
        bankName = account.bankName,
        isCash = account.getAccountType() == AccountType.CASH,
        modifier = modifier,
        size = size,
    )
}

/** [AccountAvatar] for an account that is not saved yet (the editor's live preview). */
@Composable
internal fun AccountAvatar(
    bankName: String,
    isCash: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.Icon.avatarLarge,
) {
    if (isCash) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Iconax.WalletMoney,
                contentDescription = bankName,
                modifier = Modifier.size(Dimensions.Icon.medium),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    } else {
        BrandIcon(
            merchantName = bankName,
            modifier = modifier,
            size = size,
            showBackground = true,
        )
    }
}

/** Never shrink a headline amount below this share of its natural size. */
private const val FIT_MIN_SCALE = 0.5f

/** Each shrink step while an amount is still wider than its container. */
private const val FIT_STEP = 0.9f

/**
 * A single-line money figure that shrinks to fit instead of being cut off.
 * A balance that reads "₹1,23,4…" is worse than a smaller, complete one, and
 * at large font scales a headline amount is wider than a phone card.
 */
@Composable
internal fun FittedAmountText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var fontSize by remember(text, style) { mutableStateOf(style.fontSize) }
    var fits by remember(text, style) { mutableStateOf(false) }
    Text(
        text = text,
        modifier = modifier.drawWithContent { if (fits) drawContent() },
        color = color,
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            val canShrink = fontSize.isSpecified && fontSize.value > style.fontSize.value * FIT_MIN_SCALE
            if (result.didOverflowWidth && canShrink) {
                fontSize *= FIT_STEP
            } else {
                fits = true
            }
        },
    )
}

/**
 * The round "more" button that opens an account's action menu. The disc uses
 * the plain surface colour so it stays visible on a `surfaceContainer` card.
 * [content] receives a callback that closes the menu.
 */
@Composable
internal fun AccountMoreMenu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(closeMenu: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.manage_accounts_more_options),
                modifier = Modifier.size(Dimensions.Icon.inline),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
        ) {
            content { expanded = false }
        }
    }
}

/** One entry of an [AccountMoreMenu]; [destructive] paints it in the error colour. */
@Composable
internal fun AccountMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.medium),
                tint = if (destructive) scheme.error else scheme.onSurfaceVariant,
            )
        },
        colors = if (destructive) {
            MenuDefaults.itemColors(textColor = scheme.error)
        } else {
            MenuDefaults.itemColors()
        },
    )
}

/**
 * A tonal, rounded row that looks like a field but opens something else (the
 * currency list) or only displays a value (a locked account number). [position]
 * gives it the grouped-list corners so it can sit in a stack of fields.
 */
@Composable
internal fun AccountPickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    position: ListItemPosition = ListItemPosition.Single,
    leadingIcon: ImageVector? = null,
    leadingIconDescription: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = position.toShape(),
        color = scheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .defaultMinSize(minHeight = Dimensions.Component.listItemMinHeight)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = leadingIconDescription,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = scheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                )
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The text shown for a currency in a picker: "INR  ₹". */
internal fun currencyDisplay(code: String): String =
    "$code  ${CurrencyFormatter.getCurrencySymbol(code)}"

/**
 * One selectable row of a pick-one list (currencies, merge targets): tonal when
 * idle, primary container when [selected]. The row itself carries the selected
 * semantics, so assistive tech announces the state on the control that is
 * tapped. [content] receives the title and supporting text colours that read on
 * the row's current container.
 */
@Composable
internal fun AccountChoiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    position: ListItemPosition,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(titleColor: Color, supportingColor: Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = position.toShape(),
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .defaultMinSize(minHeight = Dimensions.Component.minTouchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                content(scheme.onPrimaryContainer, scheme.onPrimaryContainer)
            } else {
                content(scheme.onSurface, scheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A bottom sheet listing every supported currency, with the current one
 * marked. Picking a row reports it and leaves closing the sheet to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountCurrencySheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currencies = remember { CurrencyFormatter.getSupportedCurrencies().sorted() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassSheetContainerColor(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.dialog,
                end = Dimensions.Padding.dialog,
                bottom = Spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.account_editor_currency_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                )
            }
            itemsIndexed(currencies, key = { _, code -> code }) { index, code ->
                val isSelected = code == selected
                AccountChoiceRow(
                    selected = isSelected,
                    onClick = { onSelect(code) },
                    position = ListItemPosition.from(index, currencies.size),
                ) { titleColor, supportingColor ->
                    Text(
                        text = code,
                        modifier = Modifier.weight(1f),
                        style = PennyWiseText.rowTitle,
                        color = titleColor,
                    )
                    Text(
                        text = CurrencyFormatter.getCurrencySymbol(code),
                        style = MaterialTheme.typography.bodyLarge,
                        color = supportingColor,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.inline),
                            tint = titleColor,
                        )
                    }
                }
            }
        }
    }
}
