package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountSelectionSheetDeviceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectingAccount_emitsTheSelectedAccount_onDevice() {
        val reserve = account(id = 2, bankName = "Reserve Bank", accountLast4 = "1357")
        var selected: AccountBalanceEntity? = null
        setContent(
            accounts = listOf(account(id = 1, bankName = "Everyday Bank", accountLast4 = "2468"), reserve),
            allowManualEntry = false,
            onAccountSelected = { selected = it },
        )

        composeTestRule.onNodeWithText("Reserve Bank ••1357").assertIsDisplayed().performClick()

        assertEquals(reserve, selected)
    }

    @Test
    fun choosingManualEntry_emitsNull_onDevice() {
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

        composeTestRule.onNodeWithText("No account (Manual Entry)")
            .assertIsDisplayed()
            .performClick()

        assertEquals(null, selected)
    }

    @Test
    fun pressingDeviceBack_invokesDismissalCallback() {
        var dismissed = false
        setContent(onDismissRequest = { dismissed = true })
        composeTestRule.onNodeWithText("Select account").assertIsDisplayed()

        pressBack()
        composeTestRule.waitForIdle()

        assertTrue("Back should dismiss the account selection sheet", dismissed)
    }

    private fun setContent(
        accounts: List<AccountBalanceEntity> = listOf(
            account(id = 1, bankName = "Everyday Bank", accountLast4 = "2468")
        ),
        selectedAccount: AccountBalanceEntity? = null,
        allowManualEntry: Boolean = false,
        onAccountSelected: (AccountBalanceEntity?) -> Unit = {},
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = false,
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
                        onDismissRequest = onDismissRequest,
                    )
                }
            }
        }
    }

    private fun account(
        id: Long,
        bankName: String,
        accountLast4: String,
    ) = AccountBalanceEntity(
        id = id,
        bankName = bankName,
        accountLast4 = accountLast4,
        balance = BigDecimal("42500.75"),
        timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
        accountType = "SAVINGS",
        currency = "INR",
    )
}
