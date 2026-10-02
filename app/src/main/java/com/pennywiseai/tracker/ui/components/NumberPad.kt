package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** The keys exposed by [NumberPad]. Kept token-based so callers cannot inject arbitrary text. */
enum class NumberPadKey(val token: String) {
    CLEAR("AC"), LEFT_PAREN("("), RIGHT_PAREN(")"), MODULO("%"), DIVIDE("÷"),
    SEVEN("7"), EIGHT("8"), NINE("9"), MULTIPLY("×"),
    FOUR("4"), FIVE("5"), SIX("6"), SUBTRACT("−"),
    ONE("1"), TWO("2"), THREE("3"), ADD("+"),
    ZERO("0"), DECIMAL("."), BACKSPACE(""), EQUALS("=")
}

/** Input state is deliberately independent from Compose, making replacement behavior testable. */
data class NumberPadInputState(
    val expression: String,
    val replaceOnNextNumber: Boolean = false
)

private val DIVISION_CONTEXT = MathContext(34, RoundingMode.HALF_EVEN)

/**
 * Evaluates the calculator grammar using BigDecimal from tokenization through the final result.
 * Division uses DECIMAL128-equivalent precision and HALF_EVEN rounding.
 */
fun evaluateNumberExpression(expression: String): BigDecimal? = runCatching {
    NumberExpressionParser(expression).parse()
}.getOrNull()

/** Stable, non-scientific rendering for the expression result. */
fun formatNumberPadResult(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    return value.stripTrailingZeros().toPlainString()
}

fun reduceNumberPadInput(state: NumberPadInputState, key: NumberPadKey): NumberPadInputState {
    val expression = state.expression
    return when (key) {
        NumberPadKey.CLEAR -> NumberPadInputState("")
        NumberPadKey.BACKSPACE -> NumberPadInputState(
            expression = expression.dropLast(1),
            replaceOnNextNumber = false
        )
        NumberPadKey.EQUALS -> {
            val result = evaluateNumberExpression(expression)
            if (result == null) state else NumberPadInputState(formatNumberPadResult(result), true)
        }
        else -> {
            val startsNumber = key in numericKeys
            val base = if (state.replaceOnNextNumber && startsNumber) "" else expression
            NumberPadInputState(
                expression = appendNumberPadToken(base, key),
                replaceOnNextNumber = false
            )
        }
    }
}

private val numericKeys = setOf(
    NumberPadKey.ZERO, NumberPadKey.ONE, NumberPadKey.TWO, NumberPadKey.THREE,
    NumberPadKey.FOUR, NumberPadKey.FIVE, NumberPadKey.SIX, NumberPadKey.SEVEN,
    NumberPadKey.EIGHT, NumberPadKey.NINE, NumberPadKey.DECIMAL
)

private fun appendNumberPadToken(expression: String, key: NumberPadKey): String {
    val token = key.token
    // Prevent two decimal points in the active number. The parser still remains the source
    // of truth for malformed expressions, but this keeps the normal keypad path friendly.
    if (key == NumberPadKey.DECIMAL) {
        val active = expression.takeLastWhile { it.isDigit() || it == '.' }
        if (active.contains('.')) return expression
        if (active.isEmpty()) return expression + "0."
    }
    return expression + token
}

@Composable
fun NumberPad(
    state: NumberPadInputState,
    onStateChange: (NumberPadInputState) -> Unit,
    modifier: Modifier = Modifier,
    resultText: String? = state.expression
        .takeIf(::containsNumberPadOperation)
        ?.let(::evaluateNumberExpression)
        ?.let(::formatNumberPadResult),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = state.expression.ifBlank { "0" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.Padding.cardCompact),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface
        )
        resultText?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.cardCompact),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        numberPadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    NumberPadButton(
                        key = key,
                        onClick = { onStateChange(reduceNumberPadInput(state, key)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

internal fun containsNumberPadOperation(expression: String): Boolean =
    expression.drop(1).any { it in "+-−×*÷/%()" } || expression.startsWith('(')

private val numberPadRows = listOf(
    listOf(NumberPadKey.CLEAR, NumberPadKey.LEFT_PAREN, NumberPadKey.RIGHT_PAREN, NumberPadKey.MODULO),
    listOf(NumberPadKey.DIVIDE, NumberPadKey.SEVEN, NumberPadKey.EIGHT, NumberPadKey.NINE),
    listOf(NumberPadKey.MULTIPLY, NumberPadKey.FOUR, NumberPadKey.FIVE, NumberPadKey.SIX),
    listOf(NumberPadKey.SUBTRACT, NumberPadKey.ONE, NumberPadKey.TWO, NumberPadKey.THREE),
    listOf(NumberPadKey.ADD, NumberPadKey.ZERO, NumberPadKey.DECIMAL, NumberPadKey.BACKSPACE),
    listOf(NumberPadKey.EQUALS)
)

@Composable
private fun NumberPadButton(
    key: NumberPadKey,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val description = when (key) {
        NumberPadKey.CLEAR -> R.string.number_pad_clear
        NumberPadKey.LEFT_PAREN -> R.string.number_pad_open_parenthesis
        NumberPadKey.RIGHT_PAREN -> R.string.number_pad_close_parenthesis
        NumberPadKey.MODULO -> R.string.number_pad_modulo
        NumberPadKey.DIVIDE -> R.string.number_pad_divide
        NumberPadKey.MULTIPLY -> R.string.number_pad_multiply
        NumberPadKey.SUBTRACT -> R.string.number_pad_subtract
        NumberPadKey.ADD -> R.string.number_pad_add
        NumberPadKey.BACKSPACE -> R.string.number_pad_backspace
        NumberPadKey.EQUALS -> R.string.number_pad_equals
        NumberPadKey.DECIMAL -> R.string.number_pad_decimal
        else -> R.string.number_pad_digit
    }
    val localizedDescription = if (key in numericKeys && key != NumberPadKey.DECIMAL) {
        stringResource(description, key.token)
    } else {
        stringResource(description)
    }
    val buttonModifier = modifier
        .height(Dimensions.Component.minTouchTarget)
        .semantics { contentDescription = localizedDescription }
    when (key) {
        NumberPadKey.CLEAR -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = CircleShape,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Text(key.token, style = MaterialTheme.typography.titleMedium) }
        NumberPadKey.EQUALS -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = CircleShape,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Text(key.token, style = MaterialTheme.typography.titleMedium) }
        NumberPadKey.BACKSPACE -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = CircleShape,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null) }
        else -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = CircleShape,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Text(key.token, style = MaterialTheme.typography.titleMedium) }
    }
}

private class NumberExpressionParser(private val input: String) {
    private var index = 0

    fun parse(): BigDecimal {
        skipWhitespace()
        if (index == input.length) error("empty expression")
        val result = parseAdditive()
        skipWhitespace()
        if (index != input.length) error("unexpected token")
        return result
    }

    private fun parseAdditive(): BigDecimal {
        var value = parseMultiplicative()
        while (true) {
            skipWhitespace()
            value = when {
                consume('+') -> value.add(parseMultiplicative())
                consume('-') || consume('−') -> value.subtract(parseMultiplicative())
                else -> return value
            }
        }
    }

    private fun parseMultiplicative(): BigDecimal {
        var value = parseUnary()
        while (true) {
            skipWhitespace()
            value = when {
                consume('*') || consume('×') -> value.multiply(parseUnary())
                consume('/') || consume('÷') -> {
                    val divisor = parseUnary()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) error("division by zero")
                    value.divide(divisor, DIVISION_CONTEXT)
                }
                consume('%') -> {
                    val divisor = parseUnary()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) error("modulo by zero")
                    value.remainder(divisor)
                }
                else -> return value
            }
        }
    }

    private fun parseUnary(): BigDecimal {
        skipWhitespace()
        return when {
            consume('+') -> parseUnary()
            consume('-') || consume('−') -> parseUnary().negate()
            else -> parsePrimary()
        }
    }

    private fun parsePrimary(): BigDecimal {
        skipWhitespace()
        if (consume('(')) {
            val value = parseAdditive()
            if (!consume(')')) error("missing close parenthesis")
            return value
        }
        val start = index
        var dots = 0
        while (index < input.length && (input[index].isDigit() || input[index] == '.')) {
            if (input[index] == '.') dots++
            index++
        }
        if (start == index || dots > 1) error("invalid number")
        return BigDecimal(input.substring(start, index))
    }

    private fun consume(char: Char): Boolean {
        if (index < input.length && input[index] == char) {
            index++
            return true
        }
        return false
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].isWhitespace()) index++
    }
}
