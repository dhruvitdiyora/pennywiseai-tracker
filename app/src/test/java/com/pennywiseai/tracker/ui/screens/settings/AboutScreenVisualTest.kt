package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AboutScreenVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun about_light() {
        setContent(darkTheme = false) {
            AboutScreenContent(
                versionName = "2.4.0-test",
                onNavigateBack = {},
                onOpenSourceCode = {},
                onNavigateToLicenses = {},
            )
        }

        composeTestRule.onNodeWithText("Version 2.4.0-test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy at a glance").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun about_dark() {
        setContent(darkTheme = true) {
            AboutScreenContent(
                versionName = "2.4.0-test",
                onNavigateBack = {},
                onOpenSourceCode = {},
                onNavigateToLicenses = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun aboutActionsAndBackRemainReachable() {
        var backCalls = 0
        var sourceCalls = 0
        var licensesCalls = 0
        setContent(darkTheme = false) {
            AboutScreenContent(
                versionName = "2.4.0-test",
                onNavigateBack = { backCalls++ },
                onOpenSourceCode = { sourceCalls++ },
                onNavigateToLicenses = { licensesCalls++ },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back to settings").performClick()
        composeTestRule.onNodeWithText("Source code").performClick()
        composeTestRule.onNodeWithText("Selected dependencies").performClick()

        assertEquals(1, backCalls)
        assertEquals(1, sourceCalls)
        assertEquals(1, licensesCalls)
    }

    @Test
    fun selectedDependencies_light() {
        setContent(darkTheme = false) {
            LicensesScreenContent(
                onNavigateBack = {},
                onDependencyClick = {},
            )
        }

        composeTestRule.onNodeWithText("AndroidX").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun selectedDependencies_dark() {
        setContent(darkTheme = true) {
            LicensesScreenContent(
                onNavigateBack = {},
                onDependencyClick = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun dependencyRowsExposeSelectionAndBackCallbacks() {
        var backCalls = 0
        var selectedName: String? = null
        setContent(darkTheme = false) {
            LicensesScreenContent(
                onNavigateBack = { backCalls++ },
                onDependencyClick = { selectedName = it.name },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back to About").performClick()
        composeTestRule.onNodeWithText("Room").performClick()

        assertEquals(1, backCalls)
        assertEquals("Room", selectedName)
    }

    @Test
    fun dependencyDialogIsAvailable() {
        setContent(darkTheme = false) {
            LicensesScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Room").performClick()
        composeTestRule
            .onNodeWithText("Licensed under the Apache License 2.0. The complete license text and dependency metadata are available from the source repository.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").performClick()
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
                Surface(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
}
