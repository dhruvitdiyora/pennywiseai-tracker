package com.pennywiseai.tracker.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class FabStackClearanceTest {

    @Test
    fun `short stack clears bottom obstruction without summing both`() {
        assertEquals(
            96.dp,
            Dimensions.Component.fabListBottomClearance(
                fabStackHeight = 0.dp,
                bottomObstruction = 80.dp,
                stackBottomInset = 16.dp,
            ),
        )
    }

    @Test
    fun `tall stack clears its measured top plus content gap`() {
        assertEquals(
            176.dp,
            Dimensions.Component.fabListBottomClearance(
                fabStackHeight = 64.dp,
                bottomObstruction = 80.dp,
                stackBottomInset = 96.dp,
            ),
        )
    }
}
