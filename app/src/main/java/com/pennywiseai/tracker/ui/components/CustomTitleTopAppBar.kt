package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * The app's one top bar: a single compact row, on every screen.
 *
 * This used to be two stacked bars — a `LargeTopAppBar` at `expandedHeight = 150.dp` (Home) or
 * `110.dp` (elsewhere) that collapsed into a regular one as you scrolled. It cost about 25% of
 * the screen before any content: the greeting sat pinned to the bottom of that 150dp with the
 * space above it empty by construction, and secondary screens spent a whole 250px line on a
 * headline-sized title. Retiring the cover banner didn't recover any of it, because the banner
 * was painted *behind* this, not under it.
 *
 * Now there is one ~56dp row. Home puts [extraInfoCard] (the greeting: avatar with its Pro
 * ring, name, and the cycle-aware subtitle) in the title slot; every other screen gets back
 * button, `titleLarge` title, and actions.
 *
 * Pass a [TopAppBarDefaults.pinnedScrollBehavior] as [scrollBehavior] and attach the same one
 * to the screen's `nestedScroll`. A pinned bar consumes no scroll delta; the
 * `exitUntilCollapsedScrollBehavior` these screens used to pass would eat the first 150dp of
 * every scroll on behalf of a bar that no longer exists.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CustomTitleTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    isHomeScreen: Boolean = false,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    extraInfoCard: @Composable () -> Unit = {},
    userName: String = "",
    profileImageUri: String? = null,
    profileBackgroundColor: Int = 0,
    hazeState: HazeState = HazeState(),
    blurEffects: Boolean = LocalBlurEffects.current
) {
    RegularTopAppBar(
        scrollBehavior = scrollBehavior,
        title = title,
        isHomeScreen = isHomeScreen,
        hasBackButton = hasBackButton,
        hasActionButton = hasActionButton,
        actionContent = actionContent,
        navigationContent = navigationContent,
        extraInfoCard = extraInfoCard,
        userName = userName,
        profileImageUri = profileImageUri,
        profileBackgroundColor = profileBackgroundColor,
        modifier = modifier,
        hazeState = hazeState,
        blurEffects = blurEffects
    )
}

@Composable
private fun Modifier.animatedOffsetModifier(
    hasBackButton: Boolean,
    hasActionButton: Boolean = false,
    isHomeScreen: Boolean = false,
): Modifier {
    val targetOffsetX = when {
        hasBackButton && hasActionButton -> 0.dp
        isHomeScreen -> 0.dp
        hasBackButton -> (-26).dp
        else -> 0.dp
    }

    val density = LocalDensity.current
    val targetOffsetXPx = with(density) { targetOffsetX.toPx() }

    val transition = updateTransition(
        targetState = Triple(hasBackButton, false, targetOffsetXPx),
        label = "offsetTransition"
    )

    val animatedOffsetX by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "offsetX"
    ) { (_, _, offset) -> offset }

    return this
        .fillMaxWidth()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(x = animatedOffsetX.toInt(), y = 0)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun RegularTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    isHomeScreen: Boolean,
    hasBackButton: Boolean = false,
    hasActionButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    extraInfoCard: @Composable () -> Unit = {},
    userName: String = "",
    profileImageUri: String? = null,
    profileBackgroundColor: Int = 0,
    hazeState: HazeState,
    blurEffects: Boolean = true
) {
    TopAppBar(
        title = {
            if (isHomeScreen) {
                // The greeting row *is* the bar. It already carries the avatar with its
                // Pro ring, the name, and the cycle-aware subtitle ("4 days left in
                // August") — none of which the old collapsed bar showed.
                extraInfoCard()
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.animatedOffsetModifier(
                        hasBackButton = hasBackButton,
                        hasActionButton = hasActionButton,
                        isHomeScreen = isHomeScreen,
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        navigationIcon = {
            BlurredAnimatedVisibility(
                visible = hasBackButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                navigationContent()
            }
        },
        actions = {
            actionContent()
        },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(0.dp),
        modifier = modifier
            .fillMaxWidth()
            // Opaque, with a shadow rather than a fade — see APP_BAR_ELEVATION.
            .shadow(elevation = APP_BAR_ELEVATION)
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    )
}


/**
 * Elevation of the app bar, which content scrolls underneath.
 *
 * The bar originally faded from `background` to fully transparent across its height, so the
 * band where the title sits had no scrim and rows read straight through it. Confining the fade
 * to the bottom 20% fixed legibility but still ghosted — a half-visible section header hovering
 * behind the title, which reads as a glitch. So the bar is opaque and separates from the
 * content with a shadow instead of by dissolving into it.
 *
 * Note this makes the bar's own haze blur redundant: an opaque background paints over it. The
 * navigation bar's blur is unaffected.
 */
private val APP_BAR_ELEVATION = Dimensions.Elevation.bottomBar
