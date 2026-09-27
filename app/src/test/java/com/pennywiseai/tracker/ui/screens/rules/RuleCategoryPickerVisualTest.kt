package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class RuleCategoryPickerVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun liveCategories_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithContentDescription("Search categories").performClick()
        composeTestRule.onNodeWithText("Select category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun filteredCategories_dark() {
        setContent(darkTheme = true)

        composeTestRule.onNodeWithContentDescription("Search categories").performClick()
        composeTestRule.onNodeWithText("Search categories").performTextInput("transport")
        composeTestRule.onNodeWithText("Transportation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertDoesNotExist()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(darkTheme: Boolean) {
        composeTestRule.setContent {
            TestTheme(darkTheme = darkTheme) {
                RuleCategoryValueInput(
                    value = "Legacy category",
                    categories = categories,
                    label = "Category Name",
                    placeholder = "e.g., Rent",
                    onValueChange = {},
                )
            }
        }
    }

    @Composable
    private fun TestTheme(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        PennyWiseTheme(
            darkTheme = darkTheme,
            dynamicColor = false,
            themeStyle = ThemeStyle.BRANDED,
            blurEffects = false,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) { content() }
        }
    }

    private companion object {
        val categories = listOf(
            CategoryEntity(id = 1, name = "Food & Dining", color = "#4CAF50"),
            CategoryEntity(id = 2, name = "Transportation", color = "#2196F3"),
            CategoryEntity(id = 3, name = "Shopping", color = "#9C27B0"),
        )
    }
}
