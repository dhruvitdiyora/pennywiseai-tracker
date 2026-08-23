package com.pennywiseai.tracker.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Shared colour helpers used across the component layer.
 *
 * These used to be private copies spread across brand, chip, and category-editing
 * components. Hoisted here (doc 20, ui-revamp) so there is exactly one definition
 * of each — a second colour-hash, parser, or contrast check drifting from this one
 * is exactly the inconsistency this file exists to prevent.
 */

/**
 * Generates a consistent colour from an arbitrary string (merchant name, category
 * name, ...) by hashing it into a small fixed palette. Deterministic: the same
 * string always maps to the same colour.
 */
internal fun generateColorFromString(str: String): Color {
    val colors = listOf(
        Color(0xFF6750A4), // Material Purple
        Color(0xFF0061A4), // Material Blue
        Color(0xFF006D40), // Material Green
        Color(0xFFB3261E), // Material Red
        Color(0xFF9A4521), // Material Orange
        Color(0xFF6D4C41), // Material Brown
        Color(0xFF455A64), // Material Blue Grey
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF43A047), // Green
        Color(0xFFE53935), // Red
    )

    val hash = str.hashCode()
    return colors[Math.abs(hash) % colors.size]
}

/**
 * Whether [color] is light enough that dark (rather than white) foreground content
 * reads with better contrast on top of it.
 */
internal fun isLightColor(color: Color): Boolean {
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return luminance > 0.5
}

/**
 * Parses a hex colour string ("#FF0000" or "FF0000") into a Compose [Color],
 * falling back to [fallback] if it isn't valid hex.
 */
internal fun parseColor(colorString: String, fallback: Color): Color {
    return try {
        val cleanColor = if (colorString.startsWith("#")) colorString else "#$colorString"
        Color(android.graphics.Color.parseColor(cleanColor))
    } catch (e: Exception) {
        fallback
    }
}
