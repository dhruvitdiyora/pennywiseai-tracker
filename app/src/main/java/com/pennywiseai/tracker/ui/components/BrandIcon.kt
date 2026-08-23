package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.icons.IconProvider
import com.pennywiseai.tracker.ui.icons.IconResource
import com.pennywiseai.tracker.ui.icons.isValidCategoryOverride

/**
 * Displays a brand icon with intelligent fallback
 */
@Composable
fun BrandIcon(
    merchantName: String,
    category: String? = null,
    subcategory: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showBackground: Boolean = true
) {
    val context = LocalContext.current
    // Every screen that has not been wired up yet supplies an empty lookup, so
    // this resolves exactly as it always did (doc 18 step 3).
    val lookup = LocalCategoryIcons.current
    val categoryEntity = lookup.category(category)
    val subcategoryEntity = lookup.subcategory(category, subcategory)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // The whole chain, memoised. `nameToResId` is already cached, but the rest
    // of the resolution would otherwise re-run on every recomposition of the
    // hottest list in the app.
    val iconResource = remember(
        merchantName, category, subcategory, categoryEntity, subcategoryEntity
    ) {
        IconProvider.getTransactionIcon(
            context = context,
            merchantName = merchantName,
            category = category,
            subcategory = subcategory,
            categoryEntity = categoryEntity,
            subcategoryEntity = subcategoryEntity
        )
    }

    val backgroundColor = when {
        iconResource is IconResource.DrawableResource -> {
            BrandIcons.getBrandColor(merchantName)?.let { Color(it.toColorInt()) }
                ?: surfaceVariant
        }
        // Same chain as the icon, so the circle and the glyph can never
        // disagree about which entity won.
        categoryEntity != null || subcategoryEntity != null -> {
            IconProvider.getTransactionIconColor(
                merchantName = merchantName,
                category = category,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                fallback = surfaceVariant
            )
        }
        category.isValidCategoryOverride() && iconResource is IconResource.VectorIcon -> {
            iconResource.tint
        }
        else -> {
            BrandIcons.getBrandColor(merchantName)?.let { Color(it.toColorInt()) }
                ?: surfaceVariant
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .clip(CircleShape)
                        .background(backgroundColor)
                        // No inner padding for drawable-backed icons: a brand
                        // logo and a category asset are finished artwork with
                        // their own margins, and 8dp more shrinks them visibly.
                        // Vector fallbacks are bare glyphs and still want it.
                        .then(
                            if (iconResource is IconResource.VectorIcon) {
                                Modifier.padding(8.dp)
                            } else {
                                Modifier
                            }
                        )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (iconResource) {
            is IconResource.DrawableResource -> {
                // Brand logo from drawable
                Image(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.TintedResIcon -> {
                // Rendered full-colour. These assets are multi-colour artwork —
                // applying a tint would flatten each one to a silhouette, which
                // is the opposite of why the user picked it.
                Image(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.VectorIcon -> {
                // Category icon fallback
                Icon(
                    imageVector = iconResource.icon,
                    contentDescription = merchantName,
                    tint = if (showBackground) Color.White else iconResource.tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Letter avatar for merchants without icons
 */
@Composable
fun LetterAvatar(
    merchantName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val letter = merchantName.firstOrNull()?.uppercase() ?: "?"
    val backgroundColor = BrandIcons.getBrandColor(merchantName)?.let { 
        Color(it.toColorInt()) 
    } ?: generateColorFromString(merchantName)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Category icon with consistent styling
 */
@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color? = null
) {
    val categoryInfo = CategoryMapping.categories[category]
        ?: CategoryMapping.categories["Others"]!!
    
    Icon(
        imageVector = categoryInfo.icon,
        contentDescription = category,
        tint = tint ?: categoryInfo.color,
        modifier = modifier.size(size)
    )
}

/**
 * Extension to convert hex string to Color Int
 */
private fun String.toColorInt(): Int {
    // Remove # if present and parse hex
    val hex = this.removePrefix("#")
    return android.graphics.Color.parseColor("#$hex")
}