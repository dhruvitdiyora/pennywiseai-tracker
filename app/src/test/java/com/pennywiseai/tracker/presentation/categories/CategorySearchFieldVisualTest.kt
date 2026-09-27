package com.pennywiseai.tracker.presentation.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
class CategorySearchFieldVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enteredQueryAndClearAction_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithText("Search categories").performTextInput("Travel")
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Clear category search").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()

        composeTestRule.onNodeWithContentDescription("Clear category search").performClick()
        composeTestRule.onNodeWithText("Travel").assertDoesNotExist()
        composeTestRule.onNodeWithText("Search categories").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+night")
    fun enteredQuery_dark() {
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Search categories").performTextInput("Coffee")
        composeTestRule.onNodeWithText("Coffee").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(darkTheme: Boolean) {
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        CategorySearchField(
                            query = query,
                            onQueryChange = { query = it },
                            onClear = { query = "" },
                            modifier = Modifier.padding(Dimensions.Padding.content),
                        )
                    }
                }
            }
        }
    }
}
