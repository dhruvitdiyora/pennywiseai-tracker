package com.pennywiseai.tracker.ui.screens.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TypingIndicatorVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun waitingState_exposesAccessibleDescription() {
        setContent(darkTheme = false)
        composeTestRule
            .onNodeWithContentDescription("Waiting for a response")
            .assertIsDisplayed()
    }

    @Test
    fun waitingIndicator_light() {
        setContent(darkTheme = false)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun waitingIndicator_dark() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(darkTheme: Boolean) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimensions.Padding.content),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TypingIndicator()
                }
            }
        }
    }
}
