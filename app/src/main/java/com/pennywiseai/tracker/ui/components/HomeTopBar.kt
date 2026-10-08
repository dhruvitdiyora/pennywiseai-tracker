package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.effects.BlurredAnimatedVisibility
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * Height of the expanded Home header — the same 150dp the shared
 * `CustomTitleTopAppBar` gives its home variant.
 */
private val HOME_BAR_EXPANDED_HEIGHT = 150.dp

/**
 * Home's collapsing header, laid out like Cashiro's: the expanded state is the
 * greeting row ([expandedContent]); scrolling fades in a compact bar with the
 * avatar at the start, the [title] centred and the round actions at the end.
 *
 * Both bars are transparent so the cover banner painted behind the Scaffold
 * shows through, and each fades to the page background at its top edge so the
 * status bar stays legible over the banner. They are drawn on top of one
 * another in the Scaffold's top-bar slot and cross-fade on scroll, exactly as
 * `CustomTitleTopAppBar` does for every other screen — Home only differs in
 * what the compact bar contains, which is why it has its own bar rather than a
 * flag on the shared one.
 *
 * @param navigationContent The compact bar's leading avatar.
 * @param actionContent The compact bar's trailing actions; lay them out in a
 *   `Row`, as the bar hands this slot a single child.
 * @param expandedContent The greeting row shown while the page is scrolled to the top.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun HomeTopBar(
    title: String,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    hazeState: HazeState,
    blurEffects: Boolean,
    navigationContent: @Composable () -> Unit,
    actionContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val collapsedFraction = scrollBehaviorLarge.state.collapsedFraction
    val pageBackground = MaterialTheme.colorScheme.background
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
    )

    // Expanded: the greeting row, fading out as the bar collapses.
    LargeTopAppBar(
        title = {
            Column(modifier = Modifier.fillMaxWidth()) { expandedContent() }
        },
        colors = barColors,
        collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight,
        expandedHeight = HOME_BAR_EXPANDED_HEIGHT,
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehaviorLarge,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = tint(backgroundColor),
                            blurRadius = 10.dp,
                            noiseFactor = -1f,
                        )
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                ) else Modifier
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(pageBackground, Color.Transparent)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(1f - collapsedFraction)
    )

    // Compact: avatar · centred title · round actions, fading in as the bar collapses.
    BlurredAnimatedVisibility(
        visible = collapsedFraction > 0.01f,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // CenterAlignedTopAppBar centres the title against the whole bar rather
        // than against whatever room is left beside the avatar, and only
        // nudges it aside when the actions genuinely need the space.
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = barColors,
            navigationIcon = navigationContent,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) { actionContent() }
            },
            scrollBehavior = scrollBehaviorSmall,
            windowInsets = WindowInsets(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = tint(pageBackground.copy(alpha = 0.6f)),
                                blurRadius = 24.dp,
                                noiseFactor = -1f,
                            )
                        }
                    ) else Modifier
                )
                // Content scrolling under the compact bar must not read through
                // beside the title (the hero's chips and chart did).
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            pageBackground,
                            pageBackground.copy(alpha = if (blurEffects) 0.55f else 0.92f)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .alpha(collapsedFraction)
        )
    }
}
