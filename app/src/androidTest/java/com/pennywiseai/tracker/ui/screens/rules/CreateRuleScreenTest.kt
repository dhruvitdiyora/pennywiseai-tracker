package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.entity.CategoryEntity
import com.pennywiseai.tracker.data.database.entity.SubcategoryEntity
import com.pennywiseai.tracker.domain.model.rule.TransactionRule
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateRuleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun subcategoryPicker_qualifiesParent_andStoresBareChildName() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        selectSetSubcategoryAction()
        clickText("Choose subcategory")
        clickText("Food")
        clickText("Monthly under Food")

        saveRule()

        assertEquals("Monthly", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun customSubcategoryPath_retainsNameOutsideCatalogue() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        selectSetSubcategoryAction()
        clickText("Enter custom subcategory")
        composeRule.onNodeWithText("Custom subcategory").performTextInput("Future child")

        saveRule()

        assertEquals("Future child", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun categoryPicker_storesParent_afterDrillingIntoChild() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        clickText("Choose category")
        clickText("Food")
        clickText("Monthly")

        saveRule()

        assertEquals("Food", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun customCategoryPath_retainsNameOutsideCatalogue() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        clickText("Enter custom category")
        composeRule.onNodeWithText("Custom category").performTextInput("Future parent")

        saveRule()

        assertEquals("Future parent", savedRule.value?.actions?.single()?.value)
    }

    private fun setScreen(savedRule: androidx.compose.runtime.MutableState<TransactionRule?>) {
        composeRule.setContent {
            PennyWiseTheme(dynamicColor = false) {
                CreateRuleScreen(
                    onNavigateBack = {},
                    onSaveRule = { savedRule.value = it },
                    categories = listOf(
                        CategoryEntity(id = 1, name = "Food", color = "#000000"),
                        CategoryEntity(id = 2, name = "Bills", color = "#000000")
                    ),
                    subcategoriesByCategory = mapOf(
                        1L to listOf(SubcategoryEntity(id = 1, categoryId = 1, name = "Monthly")),
                        2L to listOf(SubcategoryEntity(id = 2, categoryId = 2, name = "Monthly"))
                    )
                )
            }
        }
    }

    private fun selectSetSubcategoryAction() {
        clickText("Set Category")
        clickText("Set Subcategory")
    }

    private fun clickText(text: String) {
        composeRule.onNodeWithText(text).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private fun saveRule() {
        composeRule.onNodeWithText("Rule Name").performTextInput("Test rule")
        composeRule.onNodeWithText("Value").performTextInput("100")
        composeRule.onAllNodesWithText("Save")[0].performClick()
        composeRule.waitForIdle()
    }
}
