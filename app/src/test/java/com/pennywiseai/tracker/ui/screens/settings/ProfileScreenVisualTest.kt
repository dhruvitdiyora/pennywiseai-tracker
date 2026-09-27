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
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.components.ColorPickerContent
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
class ProfileScreenVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileList_light() {
        setContent(darkTheme = false) {
            ProfileScreenContent(
                profiles = sampleProfiles,
                onNavigateBack = {},
                onAddProfile = {},
                onEditProfile = {},
            )
        }

        composeTestRule.onNodeWithText("Keep each money space distinct").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun profileList_dark() {
        setContent(darkTheme = true) {
            ProfileScreenContent(
                profiles = sampleProfiles,
                onNavigateBack = {},
                onAddProfile = {},
                onEditProfile = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun addEditAndBackCallbacksRemainReachable() {
        var backCalls = 0
        var addCalls = 0
        var editedId: Long? = null
        setContent(darkTheme = false) {
            ProfileScreenContent(
                profiles = sampleProfiles,
                onNavigateBack = { backCalls++ },
                onAddProfile = { addCalls++ },
                onEditProfile = { editedId = it.id },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back to settings").performClick()
        composeTestRule
            .onNodeWithText("Add profile", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithText("Travel").performClick()

        assertEquals(1, backCalls)
        assertEquals(1, addCalls)
        assertEquals(7L, editedId)
    }

    @Test
    fun colorSwatchesExposeSelectionAndCallback() {
        var chosen = "#5E35B1"
        setContent(darkTheme = false) {
            ColorPickerContent(
                selectedColor = chosen,
                onColorChanged = { chosen = it },
            )
        }

        composeTestRule.onNodeWithContentDescription("#43A047").performClick()

        assertEquals("#43A047", chosen)
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

    private companion object {
        val sampleProfiles = listOf(
            ProfileEntity(ProfileEntity.PERSONAL_ID, "Personal", "#1E88E5", 0),
            ProfileEntity(ProfileEntity.BUSINESS_ID, "Business", "#8E24AA", 1),
            ProfileEntity(7L, "Travel", "#43A047", 2),
        )
    }
}
