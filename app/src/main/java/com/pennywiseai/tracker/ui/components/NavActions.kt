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

/**
 * One screen action drawn in the bottom bar's row. The first action is the
 * round primary button; the rest sit before it as small buttons.
 */
data class NavAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    /** Spins the icon (e.g. while an SMS scan runs). */
    val busy: Boolean = false,
    /** This button's bounds are reported to [NavActionsHost.onAnchorPositioned]. */
    val isAnchor: Boolean = false,
)

/**
 * Screen actions drawn beside the floating bottom navigation instead of as FABs
 * floating over the list, so they never cover a row.
 */
@Stable
class NavActionsHost {
    internal var owner: Any? by mutableStateOf(null)
    var actions: List<NavAction> by mutableStateOf(emptyList())
        internal set
    /** Reports the button's window bounds, e.g. for the scan spotlight. */
    var onAnchorPositioned: ((Rect) -> Unit)? by mutableStateOf(null)
        internal set
}

/** Non-null only under the main tab scaffold, where the bottom bar is drawn. */
val LocalNavActions = staticCompositionLocalOf<NavActionsHost?> { null }

/**
 * Publishes [actions] to the bottom bar while this screen is composed. Returns false when no bottom bar hosts them, so the caller should
 * draw its own buttons.
 */
@Composable
fun ProvideNavActions(
    actions: List<NavAction>,
    onAnchorPositioned: ((Rect) -> Unit)? = null,
): Boolean {
    val host = LocalNavActions.current ?: return false
    val token = remember { Any() }
    SideEffect {
        host.owner = token
        host.actions = actions
        host.onAnchorPositioned = onAnchorPositioned
    }
    DisposableEffect(host) {
        onDispose {
            // The next tab may already have claimed the host; only clear our own.
            if (host.owner === token) {
                host.owner = null
                host.actions = emptyList()
                host.onAnchorPositioned = null
            }
        }
    }
    return true
}
