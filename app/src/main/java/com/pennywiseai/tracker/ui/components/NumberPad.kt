package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.math.RoundingMode

/** Calculator-style amount entry, ported from Cashiro for account reconciliation. */
@Composable
fun NumberPad(
    initialValue: String,
    title: String,
    doneLabel: String,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expression by remember(initialValue) { mutableStateOf(initialValue) }
    var result by remember(initialValue) { mutableStateOf(initialValue.ifBlank { "0" }) }

    LaunchedEffect(expression) {
        result = expression.evaluateAmountExpression(fallback = result)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val isCalculation = expression.any { it in "+-*/%()" }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            if (isCalculation) {
                Text(
                    text = expression,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            Text(
                text = if (isCalculation) result else expression.ifBlank { "0" },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.height(Dimensions.Component.listItemMinHeight * 5)
        ) {
            items(listOf("AC", "()", "%", "/", "7", "8", "9", "*", "4", "5", "6", "-", "1", "2", "3", "+", "0", ".", "⌫", "=")) { key ->
                NumberPadButton(
                    key = key,
                    onClick = {
                        expression = when (key) {
                            "AC" -> ""
                            "⌫" -> expression.dropLast(1)
                            "=" -> result
                            "()" -> expression.nextParenthesis()
                            else -> expression.appendKey(key)
                        }
                    }
                )
            }
        }
        Button(onClick = { onDone(result) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Done, contentDescription = null)
            androidx.compose.foundation.layout.Spacer(Modifier.size(Spacing.sm))
            Text(doneLabel)
        }
    }
}

@Composable
private fun NumberPadButton(key: String, onClick: () -> Unit) {
    val isOperator = key in setOf("AC", "()", "%", "/", "*", "-", "+", "⌫", "=")
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(Dimensions.Component.listItemMinHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                key == "=" -> MaterialTheme.colorScheme.primary
                key == "AC" -> MaterialTheme.colorScheme.errorContainer
                isOperator -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when {
                key == "=" -> MaterialTheme.colorScheme.onPrimary
                key == "AC" -> MaterialTheme.colorScheme.onErrorContainer
                isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        if (key == "⌫") Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
        else Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun String.appendKey(key: String): String = when {
    key in "0123456789." -> this + key
    isNotEmpty() && last() !in "+-*/%." -> this + key
    else -> this
}

private fun String.nextParenthesis(): String {
    val open = count { it == '(' }
    val close = count { it == ')' }
    return if (open == close || lastOrNull() in setOf('+', '-', '*', '/', '%', '(')) this + "(" else this + ")"
}

private fun String.evaluateAmountExpression(fallback: String): String {
    if (isBlank()) return "0"
    return try {
        val value = SimpleMathEvaluator.evaluate(this)
        if (value.isFinite()) BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() else fallback
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

private object SimpleMathEvaluator {
    fun evaluate(input: String): Double = Parser(input).parse()

    private class Parser(private val input: String) {
        private var position = -1
        private var character = 0

        fun parse(): Double {
            next()
            val value = expression()
            if (position < input.length) throw IllegalArgumentException("Unexpected expression")
            return value
        }

        private fun next() { character = if (++position < input.length) input[position].code else -1 }

        private fun eat(expected: Int): Boolean {
            while (character == ' '.code) next()
            return (character == expected).also { if (it) next() }
        }

        private fun expression(): Double {
            var value = term()
            while (true) value = when {
                eat('+'.code) -> value + term()
                eat('-'.code) -> value - term()
                else -> return value
            }
        }

        private fun term(): Double {
            var value = factor()
            while (true) value = when {
                eat('*'.code) -> value * factor()
                eat('/'.code) -> value / factor()
                eat('%'.code) -> value % factor()
                else -> return value
            }
        }

        private fun factor(): Double {
            if (eat('+'.code)) return factor()
            if (eat('-'.code)) return -factor()
            val start = position
            return when {
                eat('('.code) -> expression().also {
                    if (!eat(')'.code)) throw IllegalArgumentException("Unclosed expression")
                }
                character in '0'.code..'9'.code || character == '.'.code -> {
                    while (character in '0'.code..'9'.code || character == '.'.code) next()
                    input.substring(start, position).toDouble()
                }
                else -> throw IllegalArgumentException("Unexpected expression")
            }
        }
    }
}
