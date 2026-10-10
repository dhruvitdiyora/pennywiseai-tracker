package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.border
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.glassFill
import com.pennywiseai.tracker.ui.components.cards.glassRim
import com.pennywiseai.tracker.ui.components.cards.toShape
import com.pennywiseai.tracker.ui.icons.BrandIcons
import com.pennywiseai.tracker.ui.theme.Dimensions

/*
 * Glass helpers for the Manage / Accounts / Categories / Rules / Chat area.
 * Thin adapters over the shared GlassCard recipe (ui/components/cards/GlassCard.kt)
 * for surfaces that are not a GlassCard — chiefly Material's ModalBottomSheet,
 * whose container colour and outline are set through parameters rather than a
 * modifier chain we own.
 */

/**
 * Container colour for a glass bottom sheet: the sheet's `surface` role at the
 * near-solid glass alpha, so the scrim and the screen behind tint through
 * faintly while text always sits on a calm fill (blur on or off).
 */
@Composable
internal fun glassSheetContainerColor(
    tint: Color = MaterialTheme.colorScheme.surface,
): Color = glassFill(tint, blurLive = false, solidFillAlpha = SHEET_FILL_ALPHA)

/**
 * A sheet lives in its own window, so Haze cannot blur what is behind it; the
 * fill stays almost opaque and the scrimmed page only faintly tints through.
 */
private const val SHEET_FILL_ALPHA = 0.96f

/**
 * Glass for a shared `GroupedRow` / `GroupedColumn` (whose surface this area
 * does not own): the row's tint at the glass alpha, passed as its
 * `containerColor`.
 */
@Composable
internal fun glassRowColor(
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
): Color = glassFill(tint, blurLive = false)

/** The glass rim round a grouped row, following its grouped-list corners. */
@Composable
internal fun Modifier.glassRowRim(position: ListItemPosition): Modifier =
    this.border(glassRim(), position.toShape())

/** The glass rim, drawn round a bottom sheet's top-rounded silhouette. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Modifier.glassSheetRim(): Modifier =
    this.border(glassRim(), BottomSheetDefaults.ExpandedShape)

/**
 * The bank's brand colour as a glass rim accent (Cashiro's bank-brand card),
 * or `null` when the bank has no known brand colour.
 */
@Composable
internal fun brandRimColor(bankName: String): Color? = remember(bankName) {
    BrandIcons.getBrandColor(bankName)?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")) }
            .getOrNull()
    }
}

/** Fill alpha for a semantic (coloured) glass container with the blur off. */
internal const val GLASS_TINTED_ALPHA = Dimensions.Glass.fillAlphaTinted
