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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategorySearchFieldDeviceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typingAndClearingQueryWorksOnDevice() {
        composeTestRule.setContent {
            var query by remember { mutableStateOf("") }
            PennyWiseTheme(
                darkTheme = true,
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

        composeTestRule.onNodeWithText("Search categories").performTextInput("Travel")
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Clear category search")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Travel").assertDoesNotExist()
        composeTestRule.onNodeWithText("Search categories").assertIsDisplayed()
    }
}
