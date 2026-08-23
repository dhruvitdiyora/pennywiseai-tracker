package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Choose subcategory").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Monthly under Food").performClick()

        saveRule()

        assertEquals("Monthly", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun customSubcategoryPath_retainsNameOutsideCatalogue() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        selectSetSubcategoryAction()
        composeRule.onNodeWithText("Enter custom subcategory").performClick()
        composeRule.onNodeWithText("Custom subcategory").performTextInput("Future child")

        saveRule()

        assertEquals("Future child", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun categoryPicker_storesParent_afterDrillingIntoChild() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        composeRule.onNodeWithText("Choose category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Monthly").performClick()

        saveRule()

        assertEquals("Food", savedRule.value?.actions?.single()?.value)
    }

    @Test
    fun customCategoryPath_retainsNameOutsideCatalogue() {
        val savedRule = mutableStateOf<TransactionRule?>(null)
        setScreen(savedRule)

        composeRule.onNodeWithText("Enter custom category").performClick()
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
        composeRule.onNodeWithText("Set Category").performClick()
        composeRule.onNodeWithText("Set Subcategory").performClick()
    }

    private fun saveRule() {
        composeRule.onNodeWithText("Rule Name").performTextInput("Test rule")
        composeRule.onNodeWithText("Value").performTextInput("100")
        composeRule.onAllNodesWithText("Save")[0].performClick()
        composeRule.waitForIdle()
    }
}
