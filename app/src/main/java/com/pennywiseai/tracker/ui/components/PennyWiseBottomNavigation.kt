package com.pennywiseai.tracker.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pennywiseai.tracker.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.pennywiseai.tracker.data.preferences.NavBarStyle
import com.pennywiseai.tracker.presentation.navigation.BottomNavItem
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun PennyWiseBottomNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    currentDestination: NavDestination?,
    navBarStyle: NavBarStyle,
    hideLabels: Boolean = false,
    hidePill: Boolean = false,
    blurEffects: Boolean = true,
    visible: Boolean = true,
    hazeState: HazeState = remember { HazeState() },
    navActions: NavActionsHost? = null,
) {
    val actions = navActions?.actions.orEmpty()
    val navigationItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Transactions,
        BottomNavItem.Analytics,
        BottomNavItem.Chat
    )
    val containerColor = MaterialTheme.colorScheme.surface
    val view = LocalView.current

    fun navigateTo(item: BottomNavItem) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        navController.navigate(item.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = modifier) {
        // NORMAL style NavigationBar
        BlurredAnimatedVisibility(
            visible = visible && navBarStyle == NavBarStyle.NORMAL,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (navActions != null && actions.isNotEmpty()) {
                    NavMoreButton(
                        host = navActions,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = Dimensions.Padding.content, bottom = Spacing.md)
                    )
                }
                HorizontalDivider(
                    thickness = Dimensions.Component.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = if (blurEffects) 0.5f else 1f
                    ),
                    tonalElevation = Dimensions.Elevation.raisedCard,
                    modifier = Modifier.then(
                        if (blurEffects) Modifier.hazeEffect(
                            state = hazeState,
                            block = fun HazeEffectScope.() {
                                style = HazeDefaults.style(
                                    backgroundColor = Color.Transparent,
                                    tint = HazeDefaults.tint(containerColor),
                                    blurRadius = 20.dp,
                                    noiseFactor = -1f,
                                )
                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                            }
                        ) else Modifier
                    )
                ) {
                    navigationItems.forEach { item ->
                        val title = stringResource(item.titleRes)
                        val selected = currentDestination?.hierarchy?.any {
                            // Match on the route base so query-arg routes (e.g.
                            // "transactions?type=…") still light up their tab.
                            it.route?.substringBefore('?') == item.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateTo(item)
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = title,
                                    tint = if (selected) {
                                        if (hidePill) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    } else MaterialTheme.colorScheme.onSurfaceVariant,
                                    // Without a pill or a label there is
                                    // nothing else marking the selected item, so
                                    // the glyph steps up to carry that weight.
                                    modifier = Modifier.size(
                                        if (hidePill && hideLabels) Dimensions.Icon.large
                                        else Dimensions.Icon.medium
                                    )
                                )
                            },
                            label = if (hideLabels) null else {
                                {
                                    Text(
                                        text = title,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (hidePill) Color.Transparent
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }

        // FLOATING style HorizontalFloatingToolbar
        BlurredAnimatedVisibility(
            visible = visible && navBarStyle == NavBarStyle.FLOATING,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
              Row(
                  modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .navigationBarsPadding(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
              ) {
                HorizontalFloatingToolbar(
                    modifier = Modifier
                        .shadow(
                            elevation = if (blurEffects) 0.dp else 16.dp,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .clip(FloatingToolbarDefaults.ContainerShape)
                        .then(
                            if (blurEffects) Modifier.hazeEffect(
                                state = hazeState,
                                block = fun HazeEffectScope.() {
                                    style = HazeDefaults.style(
                                        backgroundColor = Color.Transparent,
                                        blurRadius = 20.dp,
                                        noiseFactor = -1f,
                                    )
                                    blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                }
                            ) else Modifier
                        )
                        .zIndex(1000f),
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                            alpha = if (blurEffects) 0.7f else 1f
                        ),
                    ),
                    expanded = true,
                ) {
                    navigationItems.forEach { item ->
                        val title = stringResource(item.titleRes)
                        val selected = currentDestination?.hierarchy?.any {
                            // Match on the route base so query-arg routes (e.g.
                            // "transactions?type=…") still light up their tab.
                            it.route?.substringBefore('?') == item.route
                        } == true

                        TonalToggleButton(
                            checked = selected,
                            onCheckedChange = {
                                navigateTo(item)
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = if (blurEffects)
                                    MaterialTheme.colorScheme.surfaceBright.copy(0.6f)
                                else MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.inverseSurface,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(0.7f),
                                disabledContentColor = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                                checkedContainerColor = if (blurEffects)
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(0.6f)
                                else MaterialTheme.colorScheme.tertiaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            modifier = Modifier.padding(horizontal = Spacing.xs)
                        ) {
                            Icon(imageVector = item.icon, contentDescription = title)
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn() + expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                                exit = fadeOut() + shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec())
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier.padding(start = Spacing.sm)
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = navActions != null && actions.isNotEmpty(),
                    enter = fadeIn() + expandHorizontally(
                        expandFrom = Alignment.Start,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    ),
                    exit = fadeOut() + shrinkHorizontally(
                        shrinkTowards = Alignment.Start,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )
                ) {
                    navActions?.let { NavMoreButton(host = it) }
                }
              }
            }
        }
    }
}

/** Round "more" button; opens the current screen's [NavActionsHost.actions]. */
@Composable
private fun NavMoreButton(host: NavActionsHost, modifier: Modifier = Modifier) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.nav_actions_more)
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                expanded = true
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(Dimensions.Component.navMoreButton)
                .onGloballyPositioned { coords ->
                    host.onAnchorPositioned?.invoke(coords.boundsInWindow())
                }
                .semantics { contentDescription = description }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                if (host.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Component.navMoreButton - Spacing.sm),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(Dimensions.CornerRadius.extraLarge),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            host.actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                    },
                    onClick = {
                        expanded = false
                        action.onClick()
                    }
                )
            }
        }
    }
}
