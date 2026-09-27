package com.pennywiseai.tracker.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SettingsDataSectionsVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun manageData_light() {
        var accountsRequested = false
        setContent(darkTheme = false) {
            DataSections(onManageAccounts = { accountsRequested = true })
        }

        composeTestRule.onNodeWithText("Manage data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Accounts").performClick()
        assertTrue(accountsRequested)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun backupAndImports_dark() {
        setContent(darkTheme = true) {
            DataSections(scheduledBackupEnabled = true)
        }

        composeTestRule.onNodeWithText("Back Up Now").fetchSemanticsNode()
        composeTestRule.onNodeWithText("Imports & SMS").performScrollTo().assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Composable
    private fun DataSections(
        scheduledBackupEnabled: Boolean = false,
        onManageAccounts: () -> Unit = {},
    ) {
        SettingsDataSections(
            scheduledFolderBackupEnabled = scheduledBackupEnabled,
            scheduledFolderBackupLastTimestamp = null,
            isProEntitled = true,
            smsScanAllTime = false,
            smsScanUseCustomDate = false,
            smsScanCustomDate = null,
            smsScanMonths = 6,
            onNavigateToManageAccounts = onManageAccounts,
            onNavigateToCategories = {},
            onNavigateToRules = {},
            onNavigateToBudgets = {},
            onNavigateToLoans = {},
            onNavigateToRecurring = {},
            onNavigateToTransactionGroups = {},
            onExportData = {},
            onScheduledFolderBackupChange = {},
            onBackupNow = {},
            onChangeBackupFolder = {},
            onImportBackup = {},
            onImportCsv = {},
            onNavigateToImportStatement = {},
            onNavigateToUnrecognizedSms = {},
            onSmsScanPeriod = {},
        )
    }

    private fun setContent(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimensions.Padding.content),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                            Spacing.sm,
                        ),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
