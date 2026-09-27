package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class RuleCategoryValueInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        CategoryEntity(id = 1, name = "Food & Dining", color = "#4CAF50"),
        CategoryEntity(id = 2, name = "Transportation", color = "#2196F3")
    )

    @Test
    fun legacyValueRemainsEditableAndPickerSelectionUpdatesIt() {
        var value = "Legacy category"

        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = false,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                RuleCategoryValueInput(
                    value = value,
                    categories = categories,
                    label = "Category Name",
                    placeholder = "e.g., Rent",
                    onValueChange = { value = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Legacy category").assertTextContains("Legacy category")
        composeTestRule.onNodeWithText("Legacy category")
            .performTextReplacement("Custom fallback")
        composeTestRule.runOnIdle { assertEquals("Custom fallback", value) }

        composeTestRule.onNodeWithContentDescription("Search categories").performClick()
        composeTestRule.onNodeWithText("Transportation").performClick()
        composeTestRule.runOnIdle { assertEquals("Transportation", value) }
    }
}
