package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pennywiseai.tracker.ui.components.cards.glassFill
import com.pennywiseai.tracker.ui.theme.Dimensions

/*
 * Glass helpers for the Manage / Accounts / Categories / Rules / Chat area.
 * Thin adapters over the shared Cashiro-matching glass recipe
 * (ui/components/cards/GlassCard.kt) for surfaces that are not a GlassCard —
 * chiefly Material's ModalBottomSheet, whose container colour is set through a
 * parameter rather than a modifier chain we own.
 */

/** Container colour for a bottom sheet: Cashiro's solid `surface` role. */
@Composable
internal fun glassSheetContainerColor(
    tint: Color = MaterialTheme.colorScheme.surface,
): Color = tint

/**
 * Glass for a shared `GroupedRow` / `GroupedColumn` (whose surface this area
 * does not own): the row's tint at the glass alpha, passed as its
 * `containerColor`.
 */
@Composable
internal fun glassRowColor(
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
): Color = glassFill(tint, blurLive = false)

/** Fill alpha for a semantic (coloured) glass container with the blur off. */
internal const val GLASS_TINTED_ALPHA = Dimensions.Glass.fillAlphaTinted
