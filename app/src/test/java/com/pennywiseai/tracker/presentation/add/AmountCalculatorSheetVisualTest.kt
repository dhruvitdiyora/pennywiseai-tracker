package com.pennywiseai.tracker.presentation.add

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AmountCalculatorSheetVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exactExpressionCanBeApplied_light() {
        var appliedAmount: String? = null
        setContent(
            darkTheme = false,
            initialAmount = "12",
            onApply = { appliedAmount = it }
        )

        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Digit 3").performClick()
        composeTestRule.onNodeWithText("15").assertExists()
        composeTestRule.onRoot().captureRoboImage()
        composeTestRule.onNodeWithText("Use amount").performClick()

        assertEquals("15", appliedAmount)
    }

    @Test
    @Config(qualifiers = "+night")
    fun emptyExpressionDisablesApply_dark() {
        setContent(darkTheme = true, initialAmount = "")

        composeTestRule.onNodeWithText("Use amount").assertIsNotEnabled()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        initialAmount: String,
        onApply: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AmountCalculatorSheet(
                        initialAmount = initialAmount,
                        onDismiss = {},
                        onApply = onApply,
                    )
                }
            }
        }
    }
}
