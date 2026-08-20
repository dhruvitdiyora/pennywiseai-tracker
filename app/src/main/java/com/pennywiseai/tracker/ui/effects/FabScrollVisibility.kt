package com.pennywiseai.tracker.ui.effects

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/**
 * True while [listState] is at rest, at the top, or scrolling up — the standard rule for
 * hiding a FAB out of the way while the user reads.
 *
 * A floating FAB unavoidably covers the trailing edge of whatever row it sits over, and in a
 * money app that edge is the amount: rows rendered as "-₹2,9…" with the rest behind the
 * button. Padding the list can't fix it, because the overlap happens mid-scroll rather than at
 * the end. Getting the FAB out of the way while scrolling down does fix it, and brings it
 * straight back on the first upward flick.
 */
@Composable
fun rememberFabVisible(listState: LazyListState): State<Boolean> = remember(listState) {
    var lastIndex = listState.firstVisibleItemIndex
    var lastOffset = listState.firstVisibleItemScrollOffset
    derivedStateOf {
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        val scrollingUp = when {
            index != lastIndex -> index < lastIndex
            else -> offset <= lastOffset
        }
        lastIndex = index
        lastOffset = offset
        // At the very top there is nothing to get out of the way of.
        scrollingUp || (index == 0 && offset == 0)
    }
}
