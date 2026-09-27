package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SettingsDropdownFieldsVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun currencyAndAccountLabels_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithText("Currency", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Account", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun currencyAndAccountLabels_dark() {
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Currency", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Account", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(darkTheme: Boolean) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DropdownFields()
                    }
                }
            }
        }
    }

    @Composable
    private fun DropdownFields() {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SettingsDropdownItem(
                icon = Icons.Default.Flag,
                iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = "Default Currency",
                subtitle = "Currency used for conversions",
                fieldLabel = stringResource(R.string.settings_currency_field_label),
                currentValue = "₹ INR",
                expanded = false,
                onExpandedChange = {},
                position = com.pennywiseai.tracker.ui.components.cards.ListItemPosition.Single,
            ) {}
            SettingsDropdownItem(
                icon = Icons.Default.AccountBalanceWallet,
                iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                title = "Main Account",
                subtitle = "Sets your default currency",
                fieldLabel = stringResource(R.string.settings_account_field_label),
                currentValue = "HDFC ••••1234",
                expanded = false,
                onExpandedChange = {},
                position = com.pennywiseai.tracker.ui.components.cards.ListItemPosition.Single,
            ) {}
        }
    }
}
