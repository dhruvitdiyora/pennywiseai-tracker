package com.pennywiseai.tracker.presentation.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
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
class AddModeSwitcherVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun transactionSelected_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithText("Transaction").assertIsSelected()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun subscriptionSelectionUpdatesState_dark() {
        var callbackIndex = -1
        setContent(
            darkTheme = true,
            onSelection = { callbackIndex = it },
        )

        composeTestRule.onNodeWithText("Subscription").performClick()
        composeTestRule.onNodeWithText("Subscription").assertIsSelected()
        assertEquals(1, callbackIndex)
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        onSelection: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            var selectedIndex by remember { mutableIntStateOf(0) }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                    ) {
                        AddModeSwitcher(
                            selectedTabIndex = selectedIndex,
                            onSelectedTabChange = {
                                selectedIndex = it
                                onSelection(it)
                            },
                        )
                    }
                }
            }
        }
    }
}
