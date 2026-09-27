package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountCarouselDeviceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun balanceIsHiddenByDefault_andCanBeRevealed() {
        setContent()

        composeTestRule.onNodeWithText("••••••").assertExists()
        composeTestRule.onNodeWithText(visibleBalance).assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription("Show balance", useUnmergedTree = true)
            .performClick()

        composeTestRule.onNodeWithText(visibleBalance).assertExists()
        composeTestRule.onNodeWithText("••••••").assertDoesNotExist()
    }

    @Test
    fun balanceEyeDoesNotNavigate_butCardTapDoes() {
        var clickedAccount: Pair<String, String>? = null
        setContent { bankName, accountLast4 ->
            clickedAccount = bankName to accountLast4
        }

        composeTestRule
            .onNodeWithContentDescription("Show balance", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, clickedAccount)

        composeTestRule.onNodeWithText("View details").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Penny Bank" to "2048", clickedAccount)
    }

    private fun setContent(
        onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = false,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AccountCarousel(
                        bankAccounts = listOf(account),
                        creditCards = emptyList(),
                        onAccountClick = onAccountClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        selectedCurrency = account.currency,
                    )
                }
            }
        }
    }

    private companion object {
        const val visibleBalance = "₹42,500.75"

        val account = AccountBalanceEntity(
            bankName = "Penny Bank",
            accountLast4 = "2048",
            balance = BigDecimal("42500.75"),
            timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
            accountType = "SAVINGS",
            currency = "INR",
            alias = "Daily spending",
        )
    }
}
