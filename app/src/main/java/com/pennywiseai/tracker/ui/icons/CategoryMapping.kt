package com.pennywiseai.tracker.ui.icons

import android.content.Context
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.ui.components.parseColor
import com.pennywiseai.tracker.utils.IconResolutionUtils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pennywiseai.shared.domain.mapping.SharedCategoryMapping

/**
 * Category visual properties (icons, colors) for Android UI.
 * Keyword-based categorization is delegated to SharedCategoryMapping.
 */
object CategoryMapping {

    data class CategoryInfo(
        val icon: ImageVector,
        val color: Color,
    )

    /**
     * Resolves the display color for a category. Prefers the user's assigned color
     * ([overrideHex], e.g. "#4CAF50" from CategoryEntity), then the built-in palette,
     * then gray — so user-created categories no longer render gray in Analytics (#586).
     */
    fun colorFor(name: String, overrideHex: String? = null): Color {
        val fallback = categories[name]?.color ?: Color.Gray
        return overrideHex
            ?.takeIf { it.isNotBlank() }
            ?.let { parseColor(it, fallback) }
            ?: fallback
    }

    val categories = mapOf(
        "Food & Dining" to CategoryInfo(
            icon = Icons.Default.Restaurant,
            color = Color(0xFFFC8019),
        ),
        "Groceries" to CategoryInfo(
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF5AC85A),
        ),
        "Transportation" to CategoryInfo(
            icon = Icons.Default.DirectionsCar,
            color = Color(0xFF000000),
        ),
        "Shopping" to CategoryInfo(
            icon = Icons.Default.ShoppingBag,
            color = Color(0xFFFF9900),
        ),
        "Bills & Utilities" to CategoryInfo(
            icon = Icons.Default.Receipt,
            color = Color(0xFF4CAF50),
        ),
        "Entertainment" to CategoryInfo(
            icon = Icons.Default.MovieFilter,
            color = Color(0xFFE50914),
        ),
        "Healthcare" to CategoryInfo(
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF10847E),
        ),
        "Investments" to CategoryInfo(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFF00D09C),
        ),
        "Banking" to CategoryInfo(
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF004C8F),
        ),
        "Personal Care" to CategoryInfo(
            icon = Icons.Default.Face,
            color = Color(0xFF6A4C93),
        ),
        "Education" to CategoryInfo(
            icon = Icons.Default.School,
            color = Color(0xFF673AB7),
        ),
        "Mobile" to CategoryInfo(
            icon = Icons.Default.Smartphone,
            color = Color(0xFF2A3890),
        ),
        "Fitness" to CategoryInfo(
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFFFF3278),
        ),
        "Insurance" to CategoryInfo(
            icon = Icons.Default.Shield,
            color = Color(0xFF0066CC),
        ),
        "Tax" to CategoryInfo(
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF795548),
        ),
        "Bank Charges" to CategoryInfo(
            icon = Icons.Default.MoneyOff,
            color = Color(0xFF9E9E9E),
        ),
        "Credit Card Payment" to CategoryInfo(
            icon = Icons.Default.CreditCard,
            color = Color(0xFF1976D2),
        ),
        "Salary" to CategoryInfo(
            icon = Icons.Default.Payments,
            color = Color(0xFF4CAF50),
        ),
        "Income" to CategoryInfo(
            icon = Icons.Default.AddCircle,
            color = Color(0xFF4CAF50),
        ),
        "Travel" to CategoryInfo(
            icon = Icons.Default.Flight,
            color = Color(0xFF00BCD4),
        ),
        "Others" to CategoryInfo(
            icon = Icons.Default.Category,
            color = Color(0xFF757575),
        )
    )

    /**
     * Get category for a merchant name.
     * Delegates to SharedCategoryMapping (single source of truth).
     */
    fun getCategory(merchantName: String): String {
        return SharedCategoryMapping.getCategory(merchantName)
    }
}

/**
 * Icon provider with fallback mechanism
 */
object IconProvider {

    /**
     * Get icon for a transaction, using an explicitly set category when available.
     * 1. Try brand-specific icon
     * 2. If not found, use provided category (if valid)
     * 3. Otherwise, derive category from merchant name
     * 4. If still not found, use default icon
     */
    fun getTransactionIcon(merchantName: String, category: String?): IconResource {
        BrandIcons.getIconResource(merchantName)?.let { iconRes ->
            return IconResource.DrawableResource(iconRes)
        }

        val effectiveCategory = if (category.isValidCategoryOverride()) category
            else CategoryMapping.getCategory(merchantName)

        val categoryInfo = CategoryMapping.categories[effectiveCategory]
            ?: CategoryMapping.categories["Others"]!!

        return IconResource.VectorIcon(
            icon = categoryInfo.icon,
            tint = categoryInfo.color
        )
    }

    /**
     * Icon for a transaction, honouring the icon the **user** chose for its
     * category or subcategory (ui-revamp doc 18).
     *
     * Order: brand logo → the subcategory's own icon → the subcategory name
     * against [CategoryMapping] → the category's own icon → the hardcoded
     * category fallback → "Others".
     *
     * **The brand logo stays first on purpose.** A Swiggy row should show the
     * Swiggy logo whatever icon the user gave Food & Dining: the more specific
     * signal wins, and people read a brand mark faster than a category glyph.
     *
     * Every step is allowed to miss. A transaction can name a category that was
     * deleted, renamed, or came from another install's backup; each lookup
     * returns null and falls through. No crash, no blank circle, no log spam.
     *
     * An overload rather than a changed signature — [getTransactionIcon] has
     * callers outside the transaction row.
     */
    fun getTransactionIcon(
        context: Context,
        merchantName: String,
        category: String?,
        subcategory: String? = null,
        categoryEntity: CategoryEntity? = null,
        subcategoryEntity: SubcategoryEntity? = null,
    ): IconResource {
        BrandIcons.getIconResource(merchantName)?.let { iconRes ->
            return IconResource.DrawableResource(iconRes)
        }

        subcategoryEntity?.let { entity ->
            resolveEntityIcon(context, entity.iconName, entity.iconResId, entity.color)
                ?.let { return it }
        }

        // The subcategory *name* against the hardcoded map, before falling back
        // to the parent — "Fuel" is more specific than "Transportation".
        subcategory?.takeIf { it.isNotBlank() }?.let { name ->
            CategoryMapping.categories[name]?.let { info ->
                return IconResource.VectorIcon(info.icon, info.color)
            }
        }

        categoryEntity?.let { entity ->
            resolveEntityIcon(context, entity.iconName, entity.iconResId, entity.color)
                ?.let { return it }
        }

        return getTransactionIcon(merchantName, category)
    }

    /**
     * Circle background colour for the same chain, so the icon and its backdrop
     * never disagree about which entity won.
     */
    fun getTransactionIconColor(
        merchantName: String,
        category: String?,
        categoryEntity: CategoryEntity? = null,
        subcategoryEntity: SubcategoryEntity? = null,
        fallback: Color,
    ): Color {
        BrandIcons.getBrandColor(merchantName)?.let { return parseColor(it, fallback) }
        subcategoryEntity?.let { return parseColor(it.color, fallback) }
        categoryEntity?.let { return parseColor(it.color, fallback) }

        val effectiveCategory = if (category.isValidCategoryOverride()) category
            else CategoryMapping.getCategory(merchantName)
        return CategoryMapping.categories[effectiveCategory]?.color ?: fallback
    }

    /**
     * `iconName` first, `iconResId` only as the legacy path (doc 10's rule):
     * resource ids are renumbered between builds, names are not.
     *
     * Colour goes through the shared safe parser. Cashiro uses a bare
     * `Color.parseColor`, which **throws on a malformed hex** — one bad row
     * would take down the whole transaction list.
     */
    private fun resolveEntityIcon(
        context: Context,
        iconName: String,
        iconResId: Int,
        colorHex: String,
    ): IconResource? {
        val resId = iconName
            .takeIf { it.isNotEmpty() }
            ?.let { IconResolutionUtils.nameToResId(context, it) }
            ?.takeIf { it != 0 }
            ?: IconResolutionUtils.getSafeResId(context, iconResId, 0)

        if (resId == 0) return null
        return IconResource.TintedResIcon(resId, parseColor(colorHex, Color.Unspecified))
    }
}

/**
 * Sealed class for different icon types
 */
sealed class IconResource {
    data class DrawableResource(val resId: Int) : IconResource()
    data class VectorIcon(val icon: ImageVector, val tint: Color) : IconResource()

    /**
     * A category icon from the `type_*` drawable library (see `IconCatalog`), meant
     * to render tinted with the category's color -- unlike [DrawableResource], which
     * is a brand logo rendered at its own colours.
     *
     * Actually applying [tint] is doc 18's job; for now this renders identically to
     * [DrawableResource] (`tint = Color.Unspecified`) -- see `ui/components/BrandIcon.kt`.
     */
    data class TintedResIcon(val resId: Int, val tint: Color) : IconResource()
}

/**
 * Returns true if this category string represents a valid override
 * (non-null, non-blank, not the "Uncategorized" sentinel).
 */
internal fun String?.isValidCategoryOverride(): Boolean =
    !this.isNullOrBlank() && !this.equals("Uncategorized", ignoreCase = true)
