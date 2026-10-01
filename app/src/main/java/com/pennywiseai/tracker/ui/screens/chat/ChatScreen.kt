package com.pennywiseai.tracker.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ChatMessage
import com.pennywiseai.tracker.data.repository.ModelState
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.effects.LocalBlurEffects
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

/**
 * The AI chat screen, restyled after Cashiro's: a large collapsing title, a tonal
 * "…" action, bubble messages (user in `primaryContainer`, assistant in
 * `secondaryContainer`), and a floating pill composer that the list scrolls under.
 *
 * [onNavigateBack] is optional: Chat is a bottom-bar tab, which has no back
 * arrow, so the leading tonal button only appears when a caller supplies one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentResponse by viewModel.currentResponse.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()
    val isConfirming by viewModel.isConfirming.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val isDeveloperMode by viewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle()
    val chatStats by viewModel.chatStats.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by viewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by viewModel.totalMB.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.chat_message_copied)
    var showClearConfirmation by remember { mutableStateOf(false) }
    val blurEffects = LocalBlurEffects.current

    // Measured height of the floating composer stack, so the last message can
    // always scroll clear of it however many banners are showing above it.
    var bottomOverlayHeight by remember { mutableStateOf(0.dp) }

    val copyMessage: (ChatMessage) -> Unit = { message ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(copiedMessage, message.message))
        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
    }

    // The developer stats card is the first list item when it is showing.
    val showDeveloperCard = isDeveloperMode && messages.isNotEmpty()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, currentResponse) {
        if (messages.isNotEmpty() || currentResponse.isNotEmpty()) {
            val leadingItems = if (showDeveloperCard) 1 else 0
            scope.launch {
                listState.animateScrollToItem(
                    index = leadingItems + if (currentResponse.isNotEmpty()) messages.size else messages.size - 1
                )
            }
        }
    }

    // Large title that collapses into the compact bar as the list scrolls.
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = Dimensions.Component.bottomBarHeight)
            )
        },
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.chat_title),
                hasBackButton = onNavigateBack != null,
                navigationContent = {
                    if (onNavigateBack != null) {
                        TonalNavigationButton(
                            onClick = onNavigateBack,
                            contentDescription = stringResource(R.string.chat_back),
                        )
                    }
                },
                actionContent = {
                    if (messages.isNotEmpty()) {
                        ChatOverflowMenu(
                            hazeState = hazeState,
                            blurEffects = blurEffects,
                            onClearClick = { showClearConfirmation = true },
                        )
                    }
                },
                hazeState = hazeState,
            )
        }
    ) { paddingValues ->
        // The content runs under the (blurred) top bar, so each state adds the
        // bar's height as its own top inset rather than being padded by Scaffold.
        val topInset = paddingValues.calculateTopPadding()
        // Clears the floating bottom navigation (plus the system inset when no
        // keyboard is up), which is drawn over the screen's bottom edge.
        val bottomClearance = Dimensions.Component.bottomBarHeight + paddingValues.calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (modelState) {
                ModelState.NOT_DOWNLOADED, ModelState.DOWNLOADING, ModelState.ERROR -> {
                    val isDownloading = modelState == ModelState.DOWNLOADING
                    // Show existing messages if any, but disable input
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // If no messages, show the download prompt centered
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(top = topInset),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimensions.Icon.emptyStateContainer),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(if (isDownloading) R.string.chat_downloading_model else R.string.chat_model_required_title),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = if (isDownloading) {
                                            stringResource(
                                                R.string.chat_download_progress,
                                                downloadedMB,
                                                totalMB,
                                            )
                                        } else {
                                            stringResource(R.string.chat_model_required_body)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isDownloading) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(Spacing.sm)
                                                .clip(RoundedCornerShape(Spacing.xs)),
                                            drawStopIndicator = {}
                                        )
                                        OutlinedButton(onClick = { viewModel.cancelDownload() }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(Dimensions.Icon.small)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text(stringResource(R.string.chat_cancel))
                                        }
                                    } else {
                                        Button(onClick = { viewModel.startModelDownload() }) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(Dimensions.Icon.small)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text(stringResource(R.string.chat_download_with_size, totalMB))
                                        }
                                    }
                                    TextButton(onClick = onNavigateToSettings) {
                                        Text(stringResource(R.string.chat_settings))
                                    }
                                }
                            }
                        } else {
                            // Show existing messages (read-only)
                            ChatMessageList(
                                messages = messages,
                                listState = listState,
                                contentPadding = PaddingValues(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                    top = Dimensions.Padding.content + topInset,
                                    bottom = Spacing.lg
                                ),
                                onCopy = copyMessage,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Show model required banner at bottom, as a rounded card
                        PennyWiseCardV2(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimensions.Padding.content),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            if (isDownloading) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.chat_downloading_model),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = stringResource(R.string.chat_download_progress, downloadedMB, totalMB),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.cancelDownload() },
                                            modifier = Modifier.padding(start = Spacing.sm)
                                        ) {
                                            Text(stringResource(R.string.chat_cancel))
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(Spacing.sm)
                                            .clip(CircleShape),
                                        drawStopIndicator = {}
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.chat_model_required_short),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            // 1.6 GB via the system Download Manager; some phones (Samsung
                                            // with Data saver) won't start it on mobile data.
                                            text = stringResource(R.string.chat_model_size_hint),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.startModelDownload() },
                                        modifier = Modifier.padding(start = Spacing.sm)
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(stringResource(R.string.chat_download))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm + bottomClearance))
                    }
                }

                ModelState.READY, ModelState.LOADING -> {
                    // Show loading overlay when model is loading
                    if (modelState == ModelState.LOADING) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topInset),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.chat_initializing_model),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.chat_initializing_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // The list fills the screen; the composer stack floats over its bottom edge.
                        ChatMessageList(
                            messages = messages,
                            listState = listState,
                            contentPadding = PaddingValues(
                                start = Dimensions.Padding.content,
                                end = Dimensions.Padding.content,
                                top = Dimensions.Padding.content + topInset,
                                bottom = bottomOverlayHeight + Spacing.sm
                            ),
                            onCopy = copyMessage,
                            modifier = Modifier.fillMaxSize(),
                            leadingItems = {
                                // Developer info card
                                if (showDeveloperCard) {
                                    item {
                                        DeveloperInfoCard(chatStats = chatStats)
                                    }
                                }

                                // Example prompts when no messages
                                if (messages.isEmpty() && currentResponse.isEmpty() && !uiState.isLoading) {
                                    item {
                                        ChatEmptyState(
                                            onPromptClick = { prompt ->
                                                viewModel.sendMessage(prompt)
                                            }
                                        )
                                    }
                                }
                            },
                            trailingItems = {
                                // A transaction the model proposed — the user confirms it (#170)
                                pendingAction?.let { action ->
                                    item {
                                        PendingActionCard(
                                            action = action,
                                            currency = baseCurrency,
                                            enabled = !isConfirming,
                                            onConfirm = { viewModel.confirmPendingAction() },
                                            onDismiss = { viewModel.dismissPendingAction() }
                                        )
                                    }
                                }

                                // Show streaming response if available
                                if (currentResponse.isNotEmpty()) {
                                    item {
                                        ChatMessageItem(
                                            message = ChatMessage(
                                                message = currentResponse,
                                                isUser = false,
                                                timestamp = System.currentTimeMillis()
                                            ),
                                            isStreaming = true,
                                            onCopy = copyMessage,
                                        )
                                    }
                                } else if (uiState.isLoading) {
                                    // Show typing indicator while waiting for response
                                    item {
                                        // One status line per request, picked when the wait starts,
                                        // so a tool call's silent few seconds don't look like a hang.
                                        val status = remember(uiState.isLoading) { THINKING_LINES.random() }
                                        TypingIndicator(status = stringResource(status))
                                    }
                                }
                            }
                        )

                        val fadeHeightPx = with(density) { Spacing.xxl.toPx() }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    bottomOverlayHeight = with(density) { size.height.toDp() }
                                }
                                // Messages fade out under the stack instead of being cut off.
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 0f,
                                        endY = fadeHeightPx
                                    )
                                )
                        ) {
                            // Error message
                            AnimatedVisibility(
                                visible = uiState.error != null,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                PennyWiseCardV2(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Dimensions.Padding.content)
                                        .padding(bottom = Spacing.sm),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.error?.asString().orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.clearError() }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.chat_dismiss),
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Token limit warning
                            AnimatedVisibility(
                                visible = chatStats.contextUsagePercent >= 80,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                TokenLimitWarning(
                                    usagePercent = chatStats.contextUsagePercent,
                                    onClearChat = { showClearConfirmation = true },
                                    modifier = Modifier
                                        .padding(horizontal = Dimensions.Padding.content)
                                        .padding(bottom = Spacing.sm)
                                )
                            }

                            ChatComposer(
                                value = inputText,
                                onValueChange = { inputText = it },
                                onSend = {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    focusRequester.requestFocus()
                                },
                                enabled = !uiState.isLoading,
                                isLoading = uiState.isLoading,
                                focusRequester = focusRequester,
                                modifier = Modifier.padding(Dimensions.Padding.content),
                            )
                            Spacer(modifier = Modifier.height(bottomClearance))
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        ChatClearConfirmationDialog(
            onConfirm = {
                showClearConfirmation = false
                viewModel.clearChat()
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

/**
 * The message column shared by every model state. [leadingItems] and
 * [trailingItems] add rows before and after the messages (stats card, empty
 * state, confirm card, streaming reply, typing indicator).
 */
@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onCopy: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
    leadingItems: LazyListScope.() -> Unit = {},
    trailingItems: LazyListScope.() -> Unit = {},
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .overScrollVertical(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        flingBehavior = rememberOverscrollFlingBehavior { listState }
    ) {
        leadingItems()
        items(messages) { message ->
            ChatMessageItem(message = message, onCopy = onCopy)
        }
        trailingItems()
    }
}

/**
 * Cashiro's top-bar action: a tonal "…" disc whose menu is a rounded, blurred
 * sheet. Holds the single "Clear chat" action; clearing still goes through the
 * confirmation dialog.
 */
@Composable
private fun ChatOverflowMenu(
    hazeState: HazeState,
    blurEffects: Boolean,
    onClearClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val buttonColor = MaterialTheme.colorScheme.surfaceContainer
    val menuColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val menuShape = RoundedCornerShape(Spacing.lg)

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(end = Dimensions.Padding.content),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (blurEffects) buttonColor.copy(alpha = 0.5f) else buttonColor,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.chat_more_options),
                modifier = Modifier.size(Dimensions.Icon.inline),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(menuShape)
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(menuColor.copy(alpha = 0.5f)),
                                blurRadius = 36.dp,
                                noiseFactor = -1f,
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            containerColor = menuColor.copy(alpha = if (blurEffects) 0.7f else 1f),
            shape = menuShape,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_clear_action)) },
                onClick = {
                    expanded = false
                    onClearClick()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                },
            )
        }
    }
}
