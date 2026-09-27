package com.pennywiseai.tracker.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeLoanSummaryCardVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dual_summary_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithText("Owed to you").assertIsDisplayed()
        composeTestRule.onNodeWithText("You owe").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun dual_summary_dark() {
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Owed to you").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun whole_card_is_single_navigation_target() {
        var clicked = false
        setContent(darkTheme = false, onClick = { clicked = true })

        composeTestRule.onNode(hasClickAction()).performClick()

        assertTrue(clicked)
    }

    private fun setContent(
        darkTheme: Boolean,
        onClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        ActiveLoansSummaryCard(
                            totalLentRemaining = BigDecimal("1250.75"),
                            totalBorrowedRemaining = BigDecimal("980.50"),
                            currency = "USD",
                            onClick = onClick,
                        )
                    }
                }
            }
        }
    }
}
