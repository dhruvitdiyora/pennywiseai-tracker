package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.screens.analytics.CategoryData
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CategoryBreakdownCardVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun category_progress_rows_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithTag(CATEGORY_CARD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Food & Dining").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun category_progress_rows_dark_largeFont() {
        setContent(darkTheme = true, fontScale = 1.3f)

        composeTestRule.onNodeWithTag(CATEGORY_CARD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Groceries").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(darkTheme: Boolean, fontScale: Float = 1f) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column(modifier = Modifier.padding(Dimensions.Padding.content)) {
                            CategoryBreakdownCard(
                                categories = categories,
                                currency = "INR",
                                modifier = Modifier.testTag(CATEGORY_CARD_TAG),
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val CATEGORY_CARD_TAG = "category_breakdown_card"

        val categories = listOf(
            CategoryData(
                name = "Food & Dining",
                amount = BigDecimal("4200"),
                percentage = 48f,
                transactionCount = 12,
            ),
            CategoryData(
                name = "Groceries",
                amount = BigDecimal("2800"),
                percentage = 32f,
                transactionCount = 8,
            ),
            CategoryData(
                name = "Custom Category",
                amount = BigDecimal("1700"),
                percentage = 20f,
                transactionCount = 4,
                color = "#7B61FF",
            ),
        )
    }
}
