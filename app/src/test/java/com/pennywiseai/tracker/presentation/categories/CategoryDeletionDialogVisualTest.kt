package com.pennywiseai.tracker.presentation.categories

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.repository.CategoryDeletionImpact
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
class CategoryDeletionDialogVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun linkedRecords_light() {
        var replacementRequested = false
        setContent(darkTheme = false) {
            CategoryDeletionDialog(
                category = category,
                impact = CategoryDeletionImpact(
                    categoryId = category.id,
                    categoryName = category.name,
                    isIncome = false,
                    transactionCount = 3,
                    subscriptionCount = 1,
                    ruleConditionCount = 1,
                    ruleActionCount = 1,
                    activeBudgetCategoryCount = 1,
                ),
                isLoading = false,
                isApplying = false,
                onDismiss = {},
                onDeleteUnused = {},
                onChooseReplacement = { replacementRequested = true },
            )
        }

        composeTestRule.onNodeWithText("3 transactions, including trash").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 rule references").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
        composeTestRule.onNodeWithText("Choose replacement").performClick()
        assertTrue(replacementRequested)
    }

    @Test
    @Config(qualifiers = "+night")
    fun unusedCategory_dark() {
        var deletionRequested = false
        setContent(darkTheme = true) {
            CategoryDeletionDialog(
                category = category,
                impact = CategoryDeletionImpact(
                    categoryId = category.id,
                    categoryName = category.name,
                    isIncome = false,
                ),
                isLoading = false,
                isApplying = false,
                onDismiss = {},
                onDeleteUnused = { deletionRequested = true },
                onChooseReplacement = {},
            )
        }

        composeTestRule.onNodeWithText("This category is not used by any current records. Deleting it cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue(deletionRequested)
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

    private companion object {
        val category = CategoryEntity(
            id = 71,
            name = "Old purchases",
            color = "#795548",
        )
    }
}
