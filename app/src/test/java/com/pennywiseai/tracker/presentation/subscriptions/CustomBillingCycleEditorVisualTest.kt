package com.pennywiseai.tracker.presentation.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.domain.model.SubscriptionCycleUnit
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
class CustomBillingCycleEditorVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun threeWeeks_light() {
        setContent(darkTheme = false, count = "3", unit = SubscriptionCycleUnit.WEEK)

        composeTestRule.onNodeWithText("Custom interval").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Week").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun invalidCount_dark() {
        setContent(darkTheme = true, count = "", unit = SubscriptionCycleUnit.MONTH)

        composeTestRule.onNodeWithText("Enter a positive whole number.").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        count: String,
        unit: SubscriptionCycleUnit,
    ) {
        composeTestRule.setContent {
            TestTheme(darkTheme) {
                Column(Modifier.padding(Dimensions.Padding.content)) {
                    CustomBillingCycleEditor(
                        countInput = count,
                        unit = unit,
                        onCountChanged = {},
                        onUnitChanged = {},
                    )
                }
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}
