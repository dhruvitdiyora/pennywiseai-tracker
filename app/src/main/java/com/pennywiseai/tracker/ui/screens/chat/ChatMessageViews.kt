package com.pennywiseai.tracker.ui.screens.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.ChatMessage
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.model.PendingChatAction
import com.pennywiseai.tracker.ui.components.PennyWiseEmptyState
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.icons.iconax.Copy
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.icons.iconax.Magicpen
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Widest a chat bubble grows; shorter messages wrap their content, like Cashiro's. */
private val ChatBubbleMaxWidth = 300.dp

/**
 * Cashiro's bubble silhouette: generous corners with one tight "tail" corner on
 * the sender's side (bottom-end for the user, bottom-start for the assistant).
 * Start/end based, so it mirrors in RTL.
 */
internal fun chatBubbleShape(isUser: Boolean): RoundedCornerShape {
    val radius = Spacing.lg
    val tail = Dimensions.CornerRadius.medium
    return if (isUser) {
        RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = radius, bottomEnd = tail)
    } else {
        RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = tail, bottomEnd = radius)
    }
}

/** One status line per request is picked from these while the model is thinking. */
internal val THINKING_LINES = listOf(
    R.string.chat_thinking_reading,
    R.string.chat_thinking_working,
    R.string.chat_thinking_checking,
    R.string.chat_thinking_crunching,
    R.string.chat_thinking_one_moment
)

/**
 * A chat bubble. The user's sits at the end in `primaryContainer`, the
 * assistant's at the start in `secondaryContainer`; both carry the time and the
 * (only) per-message action, copy, in a quiet footer.
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isStreaming: Boolean = false,
    onCopy: (ChatMessage) -> Unit = {},
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val isUser = message.isUser
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val metaColor = contentColor.copy(alpha = Dimensions.Alpha.subtitle)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = ChatBubbleMaxWidth)
                .animateContentSize(),
            color = containerColor,
            contentColor = contentColor,
            shape = chatBubbleShape(isUser),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = Dimensions.Padding.card,
                    end = Dimensions.Padding.card,
                    top = Spacing.smd,
                    bottom = Spacing.xs,
                ),
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                )
                Row(
                    // As tall while streaming as once settled, so the bubble
                    // doesn't jump when streaming ends and copy appears.
                    modifier = Modifier.heightIn(min = Dimensions.Icon.large),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimensions.Icon.tiny),
                            strokeWidth = Spacing.xxs,
                            color = contentColor,
                        )
                    }
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = metaColor,
                    )
                    if (!isStreaming) {
                        IconButton(
                            onClick = { onCopy(message) },
                            modifier = Modifier.size(Dimensions.Icon.large),
                        ) {
                            Icon(
                                imageVector = Iconax.Copy,
                                contentDescription = stringResource(R.string.chat_copy_message),
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = metaColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The assistant's "thinking" bubble: pulsing dots, an optional status line, a slim indeterminate bar. */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    status: String? = null
) {
    val waitingDescription = stringResource(R.string.chat_waiting_for_response)
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.semantics { contentDescription = waitingDescription },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = contentColor,
            shape = chatBubbleShape(isUser = false),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Dimensions.Padding.card,
                    vertical = Spacing.smd,
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Three animated dots
                val infiniteTransition = rememberInfiniteTransition(label = "typing")

                for (i in 0..2) {
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 1200
                                0.3f at 0
                                1f at 400
                                0.3f at 800
                            },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(i * 200)
                        ),
                        label = "dot_alpha_$i"
                    )

                    Box(
                        modifier = Modifier
                            .size(Spacing.sm)
                            .background(
                                color = contentColor.copy(alpha = alpha),
                                shape = CircleShape
                            )
                    )
                }
                if (status != null) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }

                // Keep the waiting state legible when the animated dots are subtle in
                // either theme. This is intentionally indeterminate: it communicates
                // activity without implying a completion percentage.
                LinearProgressIndicator(
                    modifier = Modifier.size(width = Dimensions.Icon.avatarLarge, height = Spacing.xxs),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = Dimensions.Alpha.divider)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatEmptyState(
    onPromptClick: (String) -> Unit
) {
    val examplePrompts = listOf(
        stringResource(R.string.chat_example_prompt_coffee),
        stringResource(R.string.chat_example_prompt_salary),
        stringResource(R.string.chat_example_prompt_groceries),
        stringResource(R.string.chat_example_prompt_spent)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        PennyWiseEmptyState(
            icon = Iconax.Magicpen,
            headline = stringResource(R.string.chat_empty_title),
            description = stringResource(R.string.chat_empty_body)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            examplePrompts.forEach { prompt ->
                ChatPromptChip(text = prompt, onClick = { onPromptClick(prompt) })
            }
        }
    }
}

/** A tonal pill the user can tap to send an example prompt. */
@Composable
private fun ChatPromptChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimensions.CornerRadius.extraLarge),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimensions.Component.minTouchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Iconax.Magicpen,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.small),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Confirm card for an action the AI proposed (#170): add, delete or update. Nothing happens without the tap. */
@Composable
internal fun PendingActionCard(
    action: PendingChatAction,
    currency: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // A new draft is in the base currency; an existing row is shown in its own.
    val fmt = { a: BigDecimal -> CurrencyFormatter.formatCurrency(a, currency) }
    val own = { t: TransactionEntity -> CurrencyFormatter.formatCurrency(t.amount, t.currency) }
    val income = TransactionType.INCOME
    val (title, headline, detail, button) = when (action) {
        is PendingChatAction.Add -> {
            val d = action.draft
            listOf(
                stringResource(if (d.type == income) R.string.chat_action_add_income else R.string.chat_action_add_expense),
                "${fmt(d.amount)} · ${d.merchant}",
                stringResource(R.string.chat_action_add_detail, d.category, d.accountLabel),
                stringResource(R.string.chat_action_add)
            )
        }
        is PendingChatAction.Delete -> {
            val t = action.transaction
            listOf(
                stringResource(R.string.chat_action_delete_title),
                "${own(t)} · ${t.merchantName}",
                "${t.category} · ${t.dateTime.toLocalDate()}",
                stringResource(R.string.chat_action_delete)
            )
        }
        is PendingChatAction.Update -> {
            val t = action.transaction
            val changes = listOfNotNull(
                action.newMerchant?.let { stringResource(R.string.chat_action_change_merchant, it) },
                action.newCategory?.let { stringResource(R.string.chat_action_change_category, it) }
            ).joinToString(", ")
            listOf(
                stringResource(R.string.chat_action_update_title),
                "${own(t)} · ${t.merchantName}",
                "${t.category} · ${t.dateTime.toLocalDate()}\n$changes",
                stringResource(R.string.chat_action_update)
            )
        }
    }
    val isDelete = action is PendingChatAction.Delete
    PennyWiseCardV2(
        modifier = Modifier.fillMaxWidth(),
        // Same silhouette as the assistant's bubble: the proposal is part of its reply.
        shape = chatBubbleShape(isUser = false),
        colors = CardDefaults.cardColors(
            containerColor = if (isDelete) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        val fg = if (isDelete) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = fg)
            Text(text = headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fg)
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = fg)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.chat_cancel)) }
                Button(
                    onClick = onConfirm,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = if (isDelete) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) { Text(button) }
            }
        }
    }
}
