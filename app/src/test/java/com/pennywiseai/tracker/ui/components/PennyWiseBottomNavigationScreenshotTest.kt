package com.pennywiseai.tracker.ui.components

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    private lateinit var navController: NavHostController

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
        composeTestRule.waitUntil {
            navController.currentDestination?.route == "transactions"
        }

        assertEquals("transactions", navController.currentDestination?.route)
    }

    private fun setContent(navBarStyle: NavBarStyle, darkTheme: Boolean) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            composable("home") {}
                            composable("transactions") {}
                            composable("analytics") {}
                            composable("chat") {}
                        }
                        PennyWiseBottomNavigation(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            navController = navController,
                            currentDestination = currentEntry?.destination,
                            navBarStyle = navBarStyle,
                            blurEffects = false,
                        )
                    }
                }
            }
        }
    }
}
