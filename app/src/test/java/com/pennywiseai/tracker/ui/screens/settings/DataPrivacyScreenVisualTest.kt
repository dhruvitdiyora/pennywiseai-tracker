package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class DataPrivacyScreenVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun privacyOverview_light() {
        var backRequested = false
        setContent(darkTheme = false) {
            DataPrivacyScreenContent(onNavigateBack = { backRequested = true })
        }

        composeTestRule.onNodeWithText("Core tracking stays on your device").assertIsDisplayed()
        composeTestRule.onNodeWithText("What stays local").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assertTrue(backRequested)
    }

    @Test
    @Config(qualifiers = "+night")
    fun privacyOverview_dark() {
        setContent(darkTheme = true) {
            DataPrivacyScreenContent(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Core tracking stays on your device").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
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
                    content()
                }
            }
        }
    }
}
