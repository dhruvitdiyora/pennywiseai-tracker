package com.pennywiseai.tracker.ui.screens

import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.icons.iconax.DirectboxReceive
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Messages
import com.pennywiseai.tracker.ui.icons.iconax.NotificationBing
import com.pennywiseai.tracker.ui.icons.iconax.SecuritySafe
import com.pennywiseai.tracker.ui.screens.settings.SettingsIconRow
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/*
 * Shared chrome for the first-run and lock screens (Onboarding, Permission and
 * App Lock), ported from Cashiro's onboarding look: a tonal hero disc, a bold
 * centred heading, large rounded actions and tonal rows/cards. Keeping the
 * pieces here means the three screens read as one family instead of each
 * assembling its own hero, buttons and notices.
 */

/** The soft ring around the hero disc. */
private val HeroHaloSize = Dimensions.Component.onboardingIllustrationSize

/** The tonal disc that carries the hero glyph. */
private val HeroDiscSize = Dimensions.Icon.extraLarge

/** The glyph inside the hero disc. */
private val HeroGlyphSize = Dimensions.Icon.avatarLarge

/** Large actions sit at the list-row height, above the 48dp touch floor. */
private val ActionMinHeight = Dimensions.Component.listItemMinHeight

/** The halo is the disc colour washed to twice the quiet watermark strength. */
private const val HALO_ALPHA = Dimensions.Alpha.tonalIconContainer * 2f

/**
 * The scrolling, centred column every first-run step is laid out in. Short
 * content can pass `Arrangement.Center` to sit in the middle of the viewport;
 * taller content simply scrolls.
 */
@Composable
internal fun FirstRunColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .overScrollVertical()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * A tonal disc inside a faint halo: the hero art of a step. Pass [FirstRunHeroIcon]
 * (or a progress indicator) as [content]; recolour the disc with [discColor] and
 * tint the glyph with the matching `on…Container` role.
 */
@Composable
internal fun FirstRunHero(
    modifier: Modifier = Modifier,
    discColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(HeroHaloSize)
            .clip(CircleShape)
            .background(discColor.copy(alpha = HALO_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HeroDiscSize)
                .clip(CircleShape)
                .background(discColor),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** The glyph for a [FirstRunHero]. Decorative unless a [contentDescription] is given. */
@Composable
internal fun FirstRunHeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(HeroGlyphSize),
        tint = tint,
    )
}

/** The size a spinner should take inside a [FirstRunHero], matching [FirstRunHeroIcon]. */
internal val FirstRunHeroGlyphSize = HeroGlyphSize

/** Bold, centred step title with an optional supporting paragraph under it. */
@Composable
internal fun FirstRunHeading(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The step's main action: a full-width, rounded 56dp button with an optional
 * trailing glyph (a forward arrow by default). While [loading] it shows a
 * spinner in place of the label and ignores taps, so a slow write can't be
 * submitted twice.
 */
@Composable
internal fun FirstRunPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
) {
    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ActionMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.Icon.medium),
                color = LocalContentColor.current,
                strokeWidth = Dimensions.Component.progressRingStroke,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(Spacing.sm))
                Icon(imageVector = trailingIcon, contentDescription = null)
            }
        }
    }
}

/** A quieter, tonal full-width action under the primary one (Skip, open settings). */
@Composable
internal fun FirstRunSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimensions.Component.buttonHeight),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * An inline notice: a leading glyph and up to three lines of text on a tonal
 * container (an error, a rationale, a success confirmation). Pass the
 * container's matching [contentColor] so the text stays legible in light and
 * dark.
 */
@Composable
internal fun FirstRunBanner(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    hint: String? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(shape = MaterialTheme.shapes.large, tint = containerColor),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.Padding.cardCompact),
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(Dimensions.Icon.medium),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                    )
                }
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                    )
                }
                if (!hint.isNullOrBlank()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

/** A titled paragraph on a tonal card with a leading glyph (the privacy promise). */
@Composable
internal fun FirstRunInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Iconax.SecuritySafe,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tint = scheme.primaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(Dimensions.Icon.medium),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer,
                )
            }
        }
    }
}

private class PermissionRow(
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

/**
 * What the app asks for, as a grouped block of tonal rows. The notification row
 * only appears where the runtime asks for it (Android 13+), mirroring the
 * request the onboarding and permission screens actually launch.
 */
@Composable
internal fun FirstRunPermissionRows(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val rows = buildList {
        add(
            PermissionRow(
                Iconax.Messages,
                R.string.first_run_permission_read_title,
                R.string.first_run_permission_read_body,
            ),
        )
        add(
            PermissionRow(
                Iconax.DirectboxReceive,
                R.string.first_run_permission_receive_title,
                R.string.first_run_permission_receive_body,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                PermissionRow(
                    Iconax.NotificationBing,
                    R.string.first_run_permission_notifications_title,
                    R.string.first_run_permission_notifications_body,
                ),
            )
        }
    }
    GroupedList(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            SettingsIconRow(
                icon = row.icon,
                iconContainerColor = scheme.primaryContainer,
                iconContentColor = scheme.onPrimaryContainer,
                title = stringResource(row.title),
                subtitle = stringResource(row.body),
                position = ListItemPosition.from(index, rows.size),
                subtitleMaxLines = Int.MAX_VALUE,
            )
        }
    }
}
