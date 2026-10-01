package com.pennywiseai.tracker.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.icons.iconax.ExportArrow02
import com.pennywiseai.tracker.ui.icons.iconax.Ghost
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.red_light

data class FAQItem(
    @StringRes val question: Int,
    @StringRes val answer: Int
)

data class FAQCategory(
    @StringRes val title: Int,
    val icon: @Composable () -> Unit,
    val items: List<FAQItem>
)

/**
 * Help & FAQ. Each category is a titled grouped list of expandable questions
 * (the shape groups the rows, so there are no dividers), followed by a
 * report-an-issue link row. Cashiro has no in-app FAQ page — it opens a web
 * page — so this follows the grouped-row style of its Settings screens.
 */
@Composable
fun FAQScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val faqCategories = remember {
        listOf(
            FAQCategory(
                title = R.string.faq_transaction_types_section,
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = R.string.faq_wallet_credit_question,
                        answer = R.string.faq_wallet_credit_answer
                    ),
                    FAQItem(
                        question = R.string.faq_transaction_types_question,
                        answer = R.string.faq_transaction_types_answer
                    ),
                    FAQItem(
                        question = R.string.faq_transfer_vs_expense_question,
                        answer = R.string.faq_transfer_vs_expense_answer
                    )
                )
            ),
            FAQCategory(
                title = R.string.faq_sms_parsing_section,
                icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = R.string.faq_sms_not_detected_question,
                        answer = R.string.faq_sms_not_detected_answer
                    ),
                    FAQItem(
                        question = R.string.faq_unrecognized_sms_question,
                        answer = R.string.faq_unrecognized_sms_answer
                    ),
                    FAQItem(
                        question = R.string.faq_duplicates_question,
                        answer = R.string.faq_duplicates_answer
                    )
                )
            ),
            FAQCategory(
                title = R.string.faq_privacy_section,
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = R.string.faq_data_secure_question,
                        answer = R.string.faq_data_secure_answer_v2
                    ),
                    FAQItem(
                        question = R.string.faq_backup_question,
                        answer = R.string.faq_backup_answer_v2
                    ),
                    FAQItem(
                        question = R.string.faq_data_access_question,
                        answer = R.string.faq_data_access_answer_v2
                    )
                )
            ),
            FAQCategory(
                title = R.string.faq_ai_section,
                icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = R.string.faq_ai_download_question,
                        answer = R.string.faq_ai_download_answer
                    ),
                    FAQItem(
                        question = R.string.faq_ai_ask_question,
                        answer = R.string.faq_ai_ask_answer
                    )
                )
            ),
            FAQCategory(
                title = R.string.faq_accounts_section,
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                items = listOf(
                    FAQItem(
                        question = R.string.faq_manual_accounts_question,
                        answer = R.string.faq_manual_accounts_answer
                    ),
                    FAQItem(
                        question = R.string.faq_multiple_accounts_question,
                        answer = R.string.faq_multiple_accounts_answer
                    )
                )
            )
        )
    }

    var expandedItems by remember { mutableStateOf(setOf<Int>()) }

    SettingsSubScreen(
        title = stringResource(R.string.faq_title),
        backContentDescription = stringResource(R.string.faq_back),
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        // FAQ Categories
        faqCategories.forEachIndexed { categoryIndex, category ->
            SettingsSection(
                title = stringResource(category.title),
                leading = category.icon,
            ) {
                GroupedList {
                    category.items.forEachIndexed { itemIndex, faqItem ->
                        val itemKey = categoryIndex * 100 + itemIndex
                        val isExpanded = itemKey in expandedItems

                        FaqRow(
                            question = stringResource(faqItem.question),
                            answer = stringResource(faqItem.answer),
                            expanded = isExpanded,
                            position = ListItemPosition.from(itemIndex, category.items.size),
                            onToggle = {
                                expandedItems = if (isExpanded) {
                                    expandedItems - itemKey
                                } else {
                                    expandedItems + itemKey
                                }
                            }
                        )
                    }
                }
            }
        }

        // Still need help section
        SettingsSection(title = stringResource(R.string.faq_still_need_help_section)) {
            GroupedList {
                SettingsIconRow(
                    icon = Iconax.Ghost,
                    iconContainerColor = red_light,
                    iconContentColor = red_dark,
                    title = stringResource(R.string.faq_report_issue_title),
                    subtitle = stringResource(R.string.faq_report_issue_subtitle),
                    trailingIcon = Iconax.ExportArrow02,
                    position = ListItemPosition.Single,
                    onClick = {
                        openExternalLink(
                            context = context,
                            url = "${Constants.Links.GITHUB_URL}/issues/new/choose",
                            errorMessage = context.getString(R.string.about_open_link_error),
                        )
                    }
                )
            }
        }
    }
}

/**
 * One expandable question. The whole row toggles, and the answer opens inside
 * the same surface so a group keeps its connected shape while it animates.
 */
@Composable
private fun FaqRow(
    question: String,
    answer: String,
    expanded: Boolean,
    position: ListItemPosition,
    onToggle: () -> Unit,
) {
    GroupedRow(
        position = position,
        onClick = onToggle,
        minHeight = Dimensions.Component.listItemMinHeight,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = PennyWiseText.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.faq_collapse else R.string.faq_expand
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
}
