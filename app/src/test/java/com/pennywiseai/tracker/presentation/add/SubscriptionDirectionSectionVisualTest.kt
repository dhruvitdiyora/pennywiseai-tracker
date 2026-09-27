package com.pennywiseai.tracker.presentation.add

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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.SubscriptionDirection
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
class SubscriptionDirectionSectionVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expenseSelected_light() {
        setContent(darkTheme = false, direction = SubscriptionDirection.EXPENSE)

        composeTestRule.onNodeWithText("Track recurring expenses. Add the matching transaction when the payment occurs.")
            .assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun incomeSelectionInvokesCallback_dark() {
        var selectedDirection: SubscriptionDirection? = null
        setContent(
            darkTheme = true,
            direction = SubscriptionDirection.EXPENSE,
            onDirectionChange = { selectedDirection = it }
        )

        composeTestRule.onNodeWithText("Income").performClick()
        assertEquals(SubscriptionDirection.INCOME, selectedDirection)
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        direction: SubscriptionDirection,
        onDirectionChange: (SubscriptionDirection) -> Unit = {},
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
                    var renderedDirection by remember { mutableStateOf(direction) }
                    SubscriptionDirectionSection(
                        direction = renderedDirection,
                        onDirectionChange = {
                            renderedDirection = it
                            onDirectionChange(it)
                        },
                        modifier = Modifier.padding(Dimensions.Padding.content),
                    )
                }
            }
        }
    }
}
