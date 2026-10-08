package com.pennywiseai.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector

/** One entry in the bottom bar's "more" menu. */
data class NavAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Screen actions shown behind a single round "more" button beside the bottom
 * navigation (Cashiro-style), instead of FABs stacked over the list.
 */
@Stable
class NavActionsHost {
    internal var owner: Any? by mutableStateOf(null)
    var actions: List<NavAction> by mutableStateOf(emptyList())
        internal set
    /** Shows a progress ring on the button (e.g. while an SMS scan runs). */
    var busy: Boolean by mutableStateOf(false)
        internal set
    /** Reports the button's window bounds, e.g. for the scan spotlight. */
    var onAnchorPositioned: ((Rect) -> Unit)? by mutableStateOf(null)
        internal set
}

/** Non-null only under the main tab scaffold, where the bottom bar is drawn. */
val LocalNavActions = staticCompositionLocalOf<NavActionsHost?> { null }

/**
 * Publishes [actions] to the bottom bar's "more" button while this screen is
 * composed. Returns false when no bottom bar hosts them, so the caller should
 * draw its own buttons.
 */
@Composable
fun ProvideNavActions(
    actions: List<NavAction>,
    busy: Boolean = false,
    onAnchorPositioned: ((Rect) -> Unit)? = null,
): Boolean {
    val host = LocalNavActions.current ?: return false
    val token = remember { Any() }
    SideEffect {
        host.owner = token
        host.actions = actions
        host.busy = busy
        host.onAnchorPositioned = onAnchorPositioned
    }
    DisposableEffect(host) {
        onDispose {
            // The next tab may already have claimed the host; only clear our own.
            if (host.owner === token) {
                host.owner = null
                host.actions = emptyList()
                host.busy = false
                host.onAnchorPositioned = null
            }
        }
    }
    return true
}
