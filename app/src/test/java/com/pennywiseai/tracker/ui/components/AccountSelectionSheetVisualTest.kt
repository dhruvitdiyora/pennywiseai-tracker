package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AccountSelectionSheetVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectedAccount_and_manualEntry_light() {
        val selected = account(
            id = 1,
            bankName = "Everyday Bank",
            accountLast4 = "2468",
            balance = "42500.75",
            alias = "Daily spending",
        )
        setContent(
            accounts = listOf(selected, account(id = 2, bankName = "Reserve Bank", accountLast4 = "1357")),
            selectedAccount = selected,
            allowManualEntry = true,
        )

        composeTestRule.onNodeWithText("Select account").assertIsDisplayed()
        composeTestRule.onNodeWithText("No account (Manual Entry)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily spending").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Selected").assertExists()
        assertEquals(
            3,
            composeTestRule.onAllNodes(isSelectable(), useUnmergedTree = true)
                .fetchSemanticsNodes().size
        )
        composeTestRule.onNode(isSelected(), useUnmergedTree = true)
            .assertExists()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun transferPicker_hidesManualEntry_dark() {
        val selected = account(
            id = 2,
            bankName = "Reserve Bank",
            accountLast4 = "1357",
            balance = "8100",
        )
        setContent(
            accounts = listOf(
                account(id = 1, bankName = "Everyday Bank", accountLast4 = "2468"),
                selected,
            ),
            selectedAccount = selected,
            allowManualEntry = false,
            title = "Select to account",
            darkTheme = true,
        )

        composeTestRule.onNodeWithText("Select to account").assertIsDisplayed()
        composeTestRule.onNodeWithText("No account (Manual Entry)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Reserve Bank ••1357").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Selected").assertExists()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun emptyAccounts_showsEmptyState_andManualOption() {
        setContent(accounts = emptyList(), selectedAccount = null, allowManualEntry = true)

        composeTestRule.onNodeWithText("No account (Manual Entry)").assertIsDisplayed()
        composeTestRule.onNodeWithText("No accounts available").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun selectingAccount_emitsTheSelectedAccount() {
        val reserve = account(id = 2, bankName = "Reserve Bank", accountLast4 = "1357")
        var selected: AccountBalanceEntity? = null
        setContent(
            accounts = listOf(account(id = 1, bankName = "Everyday Bank", accountLast4 = "2468"), reserve),
            selectedAccount = null,
            allowManualEntry = false,
            onAccountSelected = { selected = it },
        )

        composeTestRule.onNodeWithText("Reserve Bank ••1357").performClick()

        assertEquals(reserve, selected)
    }

    @Test
    fun choosingManualEntry_emitsNull() {
        var selected: AccountBalanceEntity? = account(
            id = 1,
            bankName = "Everyday Bank",
            accountLast4 = "2468",
        )
        setContent(
            accounts = listOf(selected!!),
            selectedAccount = selected,
            allowManualEntry = true,
            onAccountSelected = { selected = it },
        )

        composeTestRule.onNodeWithText("No account (Manual Entry)").performClick()

        assertEquals(null, selected)
    }

    private fun setContent(
        accounts: List<AccountBalanceEntity>,
        selectedAccount: AccountBalanceEntity?,
        allowManualEntry: Boolean,
        title: String = "Select account",
        darkTheme: Boolean = false,
        onAccountSelected: (AccountBalanceEntity?) -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AccountSelectionSheet(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        allowManualEntry = allowManualEntry,
                        onAccountSelected = onAccountSelected,
                        onDismissRequest = {},
                        title = title,
                    )
                }
            }
        }
    }

    private fun account(
        id: Long,
        bankName: String,
        accountLast4: String,
        balance: String = "42500.75",
        alias: String? = null,
    ) = AccountBalanceEntity(
        id = id,
        bankName = bankName,
        accountLast4 = accountLast4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
        accountType = "SAVINGS",
        currency = "INR",
        alias = alias,
    )
}
