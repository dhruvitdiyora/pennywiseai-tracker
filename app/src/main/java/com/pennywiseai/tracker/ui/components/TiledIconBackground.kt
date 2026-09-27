package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing

/**
 * A deliberately quiet, brand-derived watermark for identity cards.
 * Foreground content owns contrast and semantics; this layer is decorative.
 */
@Composable
fun TiledIconBackground(
    merchantName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clearAndSetSemantics { }
            .padding(Spacing.sm),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(4) {
                    BrandIcon(
                        merchantName = merchantName,
                        size = Dimensions.Icon.list,
                        showBackground = false,
                        modifier = Modifier.alpha(Dimensions.Alpha.decorativeWatermark),
                    )
                }
            }
        }
    }
}
