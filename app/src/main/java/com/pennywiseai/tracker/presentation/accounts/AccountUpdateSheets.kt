package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.NumberPad
import com.pennywiseai.tracker.ui.components.NumberPadInputState
import com.pennywiseai.tracker.ui.components.evaluateNumberExpression
import com.pennywiseai.tracker.ui.components.formatNumberPadResult
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

/** Whole-number percentage precision shown for a credit-card utilisation preview. */
private const val UTILIZATION_SCALE = 2

/** The credit-card flow is two steps: outstanding, then limit. */
private const val CREDIT_UPDATE_STEPS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateBalanceDialog(
    bankName: String,
    accountLast4: String,
    currentBalance: BigDecimal,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var inputState by remember(currentBalance) {
        mutableStateOf(NumberPadInputState(formatNumberPadResult(currentBalance), replaceOnNextNumber = true))
    }
    val result = evaluateNumberExpression(inputState.expression)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The amount summary is tonal, so the sheet sits one step lighter.
        containerColor = glassSheetContainerColor(),
        modifier = Modifier.testTag("account_update_balance_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Dimensions.Padding.dialog,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AccountUpdateSheetHeader(
                title = stringResource(R.string.account_update_balance_title),
                bankName = bankName,
                subtitle = AccountBalanceEntity.accountLabel(bankName, accountLast4),
                onDismiss = onDismiss,
            )
            AccountUpdateAmountSummary(
                label = stringResource(R.string.account_update_balance_new),
                amount = result,
                currencyCode = currencyCode,
            )
            NumberPad(state = inputState, onStateChange = { inputState = it })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(stringResource(R.string.account_update_cancel))
                }
                Button(
                    onClick = { result?.let(onConfirm) },
                    enabled = result != null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(stringResource(R.string.account_update_update))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateCreditCardDialog(
    bankName: String,
    accountLast4: String,
    currentOutstanding: BigDecimal,
    currentLimit: BigDecimal,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var outstandingState by remember(currentOutstanding) {
        mutableStateOf(
            NumberPadInputState(
                expression = formatNumberPadResult(currentOutstanding),
                replaceOnNextNumber = true,
            )
        )
    }
    var limitState by remember(currentLimit) {
        mutableStateOf(
            NumberPadInputState(
                expression = formatNumberPadResult(currentLimit),
                replaceOnNextNumber = true,
            )
        )
    }
    val outstanding = evaluateNumberExpression(outstandingState.expression)
    val limit = evaluateNumberExpression(limitState.expression)
    val isCurrentStepValid = if (step == 1) {
        outstanding != null
    } else {
        limit != null && limit >= BigDecimal.ZERO
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassSheetContainerColor(),
        modifier = Modifier.testTag("account_update_credit_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = Dimensions.Padding.dialog,
                    end = Dimensions.Padding.dialog,
                    bottom = Dimensions.Padding.dialog,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AccountUpdateSheetHeader(
                title = stringResource(R.string.account_update_credit_title),
                bankName = bankName,
                subtitle = AccountBalanceEntity.accountLabel(bankName, accountLast4),
                onDismiss = onDismiss,
                onBack = if (step == 2) ({ step = 1 }) else null,
            )
            Text(
                text = stringResource(R.string.account_update_step_progress, step, CREDIT_UPDATE_STEPS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { step / CREDIT_UPDATE_STEPS.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.Component.progressBarHeight)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val isOutstandingStep = step == 1
            AccountUpdateAmountSummary(
                label = stringResource(
                    if (isOutstandingStep) {
                        R.string.account_update_outstanding_step
                    } else {
                        R.string.account_update_limit_step
                    }
                ),
                amount = if (isOutstandingStep) outstanding else limit,
                currencyCode = currencyCode,
                supportingText = stringResource(
                    if (!isOutstandingStep && limit != null && limit < BigDecimal.ZERO) {
                        R.string.account_update_limit_negative_error
                    } else if (isOutstandingStep) {
                        R.string.account_update_outstanding_help
                    } else {
                        R.string.account_update_limit_help
                    }
                ),
                isError = !isOutstandingStep && limit != null && limit < BigDecimal.ZERO,
            )

            NumberPad(
                state = if (isOutstandingStep) outstandingState else limitState,
                onStateChange = {
                    if (isOutstandingStep) outstandingState = it else limitState = it
                },
            )

            if (!isOutstandingStep && outstanding != null && limit != null && limit >= BigDecimal.ZERO) {
                CreditUpdatePreview(
                    outstanding = outstanding,
                    limit = limit,
                    currencyCode = currencyCode,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilledTonalButton(
                    onClick = if (step == 1) onDismiss else ({ step = 1 }),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(
                        stringResource(
                            if (step == 1) {
                                R.string.account_update_cancel
                            } else {
                                R.string.account_update_back
                            }
                        )
                    )
                }
                Button(
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else if (outstanding != null && limit != null && limit >= BigDecimal.ZERO) {
                            onConfirm(outstanding, limit)
                        }
                    },
                    enabled = isCurrentStepValid,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimensions.Component.fab),
                ) {
                    Text(
                        stringResource(
                            if (step == 1) {
                                R.string.account_update_next
                            } else {
                                R.string.account_update_update
                            }
                        )
                    )
                }
            }
        }
    }
}

/**
 * Sheet header: the account's logo, what is being updated, the account's name
 * and masked number, and round tonal Back / Close buttons.
 */
@Composable
private fun AccountUpdateSheetHeader(
    title: String,
    bankName: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.smd),
    ) {
        if (onBack != null) {
            AccountSheetIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.account_update_back),
            )
        }
        BrandIcon(
            merchantName = bankName,
            size = Dimensions.Icon.avatar,
            showBackground = true,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AccountSheetIconButton(
            onClick = onDismiss,
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.account_update_cancel),
        )
    }
}

/** A round, tonal icon button for a sheet header. */
@Composable
internal fun AccountSheetIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimensions.Icon.inline),
        )
    }
}

@Composable
private fun AccountUpdateAmountSummary(
    label: String,
    amount: BigDecimal?,
    currencyCode: String,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.card),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = amount?.let { CurrencyFormatter.formatCurrency(it, currencyCode) }
                    ?: stringResource(R.string.manage_accounts_unavailable_value),
                style = PennyWiseText.amountLarge,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun CreditUpdatePreview(
    outstanding: BigDecimal,
    limit: BigDecimal,
    currencyCode: String,
) {
    val available = limit.subtract(outstanding)
    val utilization = if (limit > BigDecimal.ZERO) {
        outstanding.max(BigDecimal.ZERO)
            .multiply(BigDecimal(100))
            .divide(limit, UTILIZATION_SCALE, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
    } else {
        stringResource(R.string.manage_accounts_unavailable_value)
    }
    val utilizationDisplay = if (limit > BigDecimal.ZERO) {
        stringResource(R.string.manage_accounts_utilization_percent, utilization)
    } else {
        utilization
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.cardCompact),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AccountUpdatePreviewRow(
                label = stringResource(R.string.account_update_available_credit),
                value = CurrencyFormatter.formatCurrency(available, currencyCode),
            )
            AccountUpdatePreviewRow(
                label = stringResource(R.string.account_update_utilization),
                value = utilizationDisplay,
            )
        }
    }
}

@Composable
private fun AccountUpdatePreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
