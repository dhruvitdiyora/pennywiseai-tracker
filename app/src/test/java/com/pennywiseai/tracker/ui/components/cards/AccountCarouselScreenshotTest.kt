package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AccountCarouselScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun savings_light() {
        setContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
                alias = "Daily spending",
            ),
            darkTheme = false,
        )

        composeTestRule
            .onNodeWithContentDescription("Show balance", useUnmergedTree = true)
            .performClick()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun credit_dark() {
        setContent(
            account = account(
                bankName = "Penny Card",
                accountLast4 = "9912",
                balance = "12850.25",
                creditLimit = "50000",
                isCreditCard = true,
            ),
            darkTheme = true,
        )

        composeTestRule
            .onNodeWithContentDescription("Show balance", useUnmergedTree = true)
            .performClick()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun lowBalance_light() {
        setContent(
            account = account(
                bankName = "Penny Reserve",
                accountLast4 = "7331",
                balance = "850.25",
                alias = "Emergency fund",
                lowBalanceThreshold = "1000",
            ),
            darkTheme = false,
        )

        composeTestRule
            .onNodeWithContentDescription("Show balance", useUnmergedTree = true)
            .performClick()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun multipleAccounts_light() {
        setContent(
            bankAccounts = listOf(
                account(
                    bankName = "Penny Bank",
                    accountLast4 = "2048",
                    balance = "42500.75",
                    alias = "Daily spending",
                ),
                account(
                    bankName = "Reserve Bank",
                    accountLast4 = "1357",
                    balance = "18200",
                    alias = "Rainy day",
                ),
            ),
            darkTheme = false,
        )

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun currentAccountUsesStoredAccountType() {
        setContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
                accountType = "CURRENT",
            ),
            darkTheme = false,
        )

        composeTestRule.onNodeWithText("Current").assertExists()
    }

    private fun setContent(account: AccountBalanceEntity, darkTheme: Boolean) {
        setContent(
            bankAccounts = if (account.isCreditCard) emptyList() else listOf(account),
            creditCards = if (account.isCreditCard) listOf(account) else emptyList(),
            darkTheme = darkTheme,
        )
    }

    private fun setContent(
        bankAccounts: List<AccountBalanceEntity>,
        creditCards: List<AccountBalanceEntity> = emptyList(),
        darkTheme: Boolean,
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
                    Column(modifier = Modifier.padding(Dimensions.Padding.content)) {
                        AccountCarousel(
                            bankAccounts = bankAccounts,
                            creditCards = creditCards,
                            modifier = Modifier.fillMaxWidth(),
                            selectedCurrency = (bankAccounts + creditCards).first().currency,
                        )
                    }
                }
            }
        }
    }

    private fun account(
        bankName: String,
        accountLast4: String,
        balance: String,
        alias: String? = null,
        accountType: String? = null,
        creditLimit: String? = null,
        isCreditCard: Boolean = false,
        lowBalanceThreshold: String? = null,
    ) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = accountLast4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
        creditLimit = creditLimit?.let(::BigDecimal),
        isCreditCard = isCreditCard,
        accountType = accountType,
        currency = "INR",
        alias = alias,
        lowBalanceThreshold = lowBalanceThreshold?.let(::BigDecimal),
    )
}
