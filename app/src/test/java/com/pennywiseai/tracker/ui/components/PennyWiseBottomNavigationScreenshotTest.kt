package com.pennywiseai.tracker.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.NavBarStyle
import com.pennywiseai.tracker.data.preferences.ThemeStyle
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
class PennyWiseBottomNavigationScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var selectedRoute by mutableStateOf("home")

    @Test
    fun normal_light() {
        setContent(navBarStyle = NavBarStyle.NORMAL, darkTheme = false)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun floating_dark() {
        setContent(navBarStyle = NavBarStyle.FLOATING, darkTheme = true)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun transactionsTapKeepsTheExistingRouteContract() {
        setContent(navBarStyle = NavBarStyle.NORMAL, darkTheme = false)

        composeTestRule
            .onNodeWithContentDescription("Transactions", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitUntil { selectedRoute == "transactions" }

        assertEquals("transactions", selectedRoute)
    }

    private fun setContent(navBarStyle: NavBarStyle, darkTheme: Boolean) {
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        PennyWiseBottomNavigation(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            selectedRoute = selectedRoute,
                            onTabSelected = { selectedRoute = it.route },
                            navBarStyle = navBarStyle,
                            blurEffects = false,
                        )
                    }
                }
            }
        }
    }
}
