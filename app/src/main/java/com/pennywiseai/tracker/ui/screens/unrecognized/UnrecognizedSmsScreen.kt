package com.pennywiseai.tracker.ui.screens.unrecognized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.UnrecognizedSmsEntity
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.SubtitleTag
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.skeleton.TransactionItemSkeleton
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.Bag
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.MessageQuestion
import com.pennywiseai.tracker.ui.icons.iconax.Send
import com.pennywiseai.tracker.ui.screens.rules.UtilityHeroCard
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

/*
 * Unrecognized SMS, in the Cashiro style: a large collapsing title with a tonal
 * back button, a primary hero card carrying the "show reported" filter and the
 * counts, and each message as a card with the sender, a quoted body and the
 * delete / report actions. Reporting and deleting behave exactly as before.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnrecognizedSmsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    viewModel: UnrecognizedSmsViewModel = hiltViewModel()
) {
    val unrecognizedMessages by viewModel.unrecognizedMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showReported by viewModel.showReported.collectAsStateWithLifecycle()
    var selectedMessage by remember { mutableStateOf<UnrecognizedSmsEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    // Staggered entrance animation state — only animates on first composition
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 30.dp.roundToPx() }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            delay(600)
            hasAnimated = true
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.unrecognized_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.unrecognized_back)
                    )
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState }
        ) {
            // Header card with info (0ms delay)
            item {
                val visible = remember { mutableStateOf(hasAnimated) }
                LaunchedEffect(Unit) {
                    if (!hasAnimated) { delay(0); visible.value = true }
                }
                AnimatedVisibility(
                    visible = visible.value,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { slideOffsetPx },
                        animationSpec = tween(300)
                    )
                ) {
                    UtilityHeroCard(
                        icon = Iconax.MessageQuestion,
                        title = stringResource(R.string.unrecognized_header_title),
                        body = stringResource(R.string.unrecognized_header_body)
                    ) {
                        // Filter toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            ShowReportedChip(
                                selected = showReported,
                                onClick = { viewModel.toggleShowReported() }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (unrecognizedMessages.isNotEmpty()) {
                                val reportedCount = unrecognizedMessages.count { it.reported }
                                val unreportedCount = unrecognizedMessages.size - reportedCount

                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    if (unreportedCount > 0) {
                                        CountPill(
                                            text = pluralStringResource(
                                                R.plurals.unrecognized_new_count,
                                                unreportedCount,
                                                unreportedCount
                                            ),
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    if (reportedCount > 0) {
                                        CountPill(
                                            text = pluralStringResource(
                                                R.plurals.unrecognized_reported_count,
                                                reportedCount,
                                                reportedCount
                                            ),
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Loading State — skeleton items
            if (isLoading) {
                items(4) {
                    UnrecognizedSmsItemSkeleton()
                }
            } else if (unrecognizedMessages.isEmpty()) {
                // Empty State
                item {
                    PennyWiseEmptyState(
                        icon = Icons.Outlined.MarkEmailRead,
                        headline = stringResource(R.string.unrecognized_empty_title),
                        description = stringResource(R.string.unrecognized_empty_body)
                    )
                }
            } else {
                // Message items with staggered animation (50ms per item, starting at 50ms)
                itemsIndexed(
                    items = unrecognizedMessages,
                    key = { _, message -> message.id }
                ) { index, message ->
                    val visible = remember { mutableStateOf(hasAnimated) }
                    LaunchedEffect(Unit) {
                        if (!hasAnimated) { delay((index + 1) * 50L); visible.value = true }
                    }
                    AnimatedVisibility(
                        visible = visible.value,
                        enter = fadeIn(tween(300)) + slideInVertically(
                            initialOffsetY = { slideOffsetPx },
                            animationSpec = tween(300)
                        )
                    ) {
                        UnrecognizedSmsItem(
                            message = message,
                            onReport = {
                                viewModel.reportMessage(message)
                            },
                            onDelete = {
                                selectedMessage = message
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                selectedMessage = null
            },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Icon(
                    Iconax.Bag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.unrecognized_delete_title)) },
            text = {
                Text(stringResource(R.string.unrecognized_delete_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedMessage?.let { viewModel.deleteMessage(it) }
                        showDeleteConfirmation = false
                        selectedMessage = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.unrecognized_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        selectedMessage = null
                    }
                ) {
                    Text(stringResource(R.string.unrecognized_cancel))
                }
            }
        )
    }
}

/**
 * The "show reported" filter, drawn for the primary-container hero card: the
 * selected state is a solid primary chip (a primary-container chip would
 * vanish into the card), idle is the page surface.
 */
@Composable
private fun ShowReportedChip(
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.unrecognized_show_reported)) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.Icon.small)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surface,
            labelColor = scheme.onSurface,
            selectedContainerColor = scheme.primary,
            selectedLabelColor = scheme.onPrimary,
            selectedLeadingIconColor = scheme.onPrimary
        ),
        border = null
    )
}

/** A small count on the hero card ("3 new", "2 reported"). */
@Composable
private fun CountPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun UnrecognizedSmsItem(
    message: UnrecognizedSmsEntity,
    onReport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.smd)
        ) {
            // Header with sender and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Iconax.MessageQuestion,
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                    size = Dimensions.Icon.list,
                    glyphSize = Dimensions.Icon.inline
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.receivedAt.format(
                            DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm")
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                if (message.reported) {
                    SubtitleTag(
                        text = stringResource(R.string.unrecognized_reported_badge),
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            // Message content (truncated), set apart like a quoted message
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = scheme.surface
            ) {
                Text(
                    text = message.smsBody,
                    modifier = Modifier.padding(Spacing.smd),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = scheme.error
                    )
                ) {
                    Icon(
                        Iconax.Bag,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.Icon.small)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.unrecognized_delete))
                }

                if (!message.reported) {
                    Spacer(modifier = Modifier.width(Spacing.sm))

                    FilledTonalButton(
                        onClick = onReport,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = scheme.primaryContainer,
                            contentColor = scheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Iconax.Send,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.unrecognized_report))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnrecognizedSmsItemSkeleton(
    modifier: Modifier = Modifier
) {
    TransactionItemSkeleton(modifier = modifier)
}
