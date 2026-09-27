package com.pennywiseai.tracker.ui.components

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberPadTest {

    @Test
    fun `evaluation stays exact and respects precedence and parentheses`() {
        assertDecimalEquals("0.3", evaluateNumberExpression("0.1 + 0.2"))
        assertDecimalEquals("7", evaluateNumberExpression("1 + 2 × 3"))
        assertDecimalEquals("9", evaluateNumberExpression("(1 + 2) × 3"))
    }

    @Test
    fun `division is decimal128 and invalid operations are rejected`() {
        assertEquals(
            "0.3333333333333333333333333333333333",
            formatNumberPadResult(evaluateNumberExpression("1 ÷ 3")!!),
        )
        assertNull(evaluateNumberExpression("1 ÷ 0"))
        assertNull(evaluateNumberExpression("1 +"))
        assertNull(evaluateNumberExpression("(1 + 2"))
    }

    @Test
    fun `percent key has explicit remainder semantics`() {
        assertDecimalEquals("1", evaluateNumberExpression("10 % 3"))
        assertNull(evaluateNumberExpression("10 % 0"))
    }

    @Test
    fun `unary negative values and plain formatting are supported`() {
        assertDecimalEquals("-25.5", evaluateNumberExpression("−25.5"))
        assertEquals("100000000000000000000", formatNumberPadResult(BigDecimal("1E+20")))
        assertEquals("0", formatNumberPadResult(BigDecimal("0.000")))
    }

    @Test
    fun `first number replaces seeded value while operator continues it`() {
        val seeded = NumberPadInputState("125.50", replaceOnNextNumber = true)

        assertEquals(
            NumberPadInputState("7"),
            reduceNumberPadInput(seeded, NumberPadKey.SEVEN),
        )
        assertEquals(
            NumberPadInputState("125.50+"),
            reduceNumberPadInput(seeded, NumberPadKey.ADD),
        )
    }

    @Test
    fun `equals replaces on next number and decimal input remains well formed`() {
        val evaluated = reduceNumberPadInput(NumberPadInputState("2+3"), NumberPadKey.EQUALS)
        assertEquals(NumberPadInputState("5", replaceOnNextNumber = true), evaluated)
        assertEquals(
            NumberPadInputState("9"),
            reduceNumberPadInput(evaluated, NumberPadKey.NINE),
        )

        val decimal = reduceNumberPadInput(NumberPadInputState(""), NumberPadKey.DECIMAL)
        assertEquals("0.", decimal.expression)
        assertEquals(
            decimal,
            reduceNumberPadInput(decimal, NumberPadKey.DECIMAL),
        )
    }

    private fun assertDecimalEquals(expected: String, actual: BigDecimal?) {
        assertTrue("Expected a value", actual != null)
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
}
