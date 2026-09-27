package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
class AppearanceChoiceTileVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectedChoiceHasRadioSemantics_light() {
        setContent(darkTheme = false, selected = true)

        composeTestRule.onNodeWithTag("appearance_choice").assertIsSelected()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun unselectedChoiceSupportsLargeFontAndClick_dark() {
        var clicks = 0
        setContent(
            darkTheme = true,
            selected = false,
            fontScale = 2f,
            onClick = { clicks++ }
        )

        composeTestRule.onNodeWithTag("appearance_choice")
            .assertIsNotSelected()
            .performClick()
        assertEquals(1, clicks)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [30])
    fun themeStyleHidesDynamicBeforeAndroid12() {
        setThemeStyleSelector()

        composeTestRule.onNodeWithText("Dynamic").assertDoesNotExist()
        composeTestRule.onNodeWithText("Default").assertExists()
    }

    @Test
    @Config(sdk = [35])
    fun themeStyleOffersDynamicOnAndroid12() {
        var selectedStyle: ThemeStyle? = null
        setThemeStyleSelector(onStyleSelected = { selectedStyle = it })

        composeTestRule.onNodeWithText("Dynamic")
            .assertExists()
            .performClick()
        assertEquals(ThemeStyle.DYNAMIC, selectedStyle)
    }

    private fun setContent(
        darkTheme: Boolean,
        selected: Boolean,
        fontScale: Float = 1f,
        onClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppearanceChoiceTile(
                                title = "Dynamic",
                                subtitle = "Wallpaper colors",
                                selected = selected,
                                index = 0,
                                count = 2,
                                onClick = onClick,
                                icon = Icons.Default.AutoAwesome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimensions.Padding.content)
                                    .testTag("appearance_choice"),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setThemeStyleSelector(
        onStyleSelected: (ThemeStyle) -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = false,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ThemeStyleSelector(
                        currentStyle = ThemeStyle.BRANDED,
                        onStyleSelected = onStyleSelected,
                    )
                }
            }
        }
    }
}
