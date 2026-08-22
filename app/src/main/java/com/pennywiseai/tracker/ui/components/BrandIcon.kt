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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showBackground: Boolean = true
) {
    val iconResource = IconProvider.getTransactionIcon(merchantName, category)

    val backgroundColor = if (category.isValidCategoryOverride()
        && iconResource !is IconResource.DrawableResource
    ) {
        (iconResource as IconResource.VectorIcon).tint
    } else {
        val brandColor = BrandIcons.getBrandColor(merchantName)
        brandColor?.let { Color(it.toColorInt()) }
            ?: MaterialTheme.colorScheme.surfaceVariant
    }
    
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .padding(8.dp)
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
                // Same body as DrawableResource for now -- applying the tint is
                // doc 18's job (see IconResource.TintedResIcon kdoc).
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