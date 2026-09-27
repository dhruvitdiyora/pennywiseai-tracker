package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.CardEntity
import com.pennywiseai.tracker.data.database.entity.CardType
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
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
class ManageAccountCardScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun savingsWithAliasAndLinkedCard_light() {
        setAccountContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
                alias = "Daily spending",
                profileId = ProfileEntity.BUSINESS_ID,
            ),
            linkedCards = listOf(
                CardEntity(
                    id = 7,
                    cardLast4 = "7319",
                    cardType = CardType.DEBIT,
                    bankName = "Penny Bank",
                    accountLast4 = "2048",
                    lastBalanceDate = LocalDateTime.of(2026, 9, 1, 9, 30),
                ),
            ),
            darkTheme = false,
        )

        // The foreground brand avatar is announced once; the tiled motif is decorative.
        composeTestRule.onNodeWithContentDescription("Penny Bank").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun creditCardWithLimit_dark() {
        setCreditContent(
            card = account(
                bankName = "Penny Card",
                accountLast4 = "9912",
                balance = "12850.25",
                creditLimit = "50000",
                isCreditCard = true,
            ),
            darkTheme = true,
        )

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun manualAccountPrimaryActionAndMenuKeepTheirCallbacks() {
        var editCalls = 0
        var updateCalls = 0
        var historyCalls = 0
        var unlinkId: Long? = null
        val linkedCard = CardEntity(
            id = 7,
            cardLast4 = "7319",
            cardType = CardType.DEBIT,
            bankName = "Penny Bank",
            accountLast4 = "2048",
        )

        setAccountContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
                alias = "Daily spending",
                sourceType = "MANUAL",
            ),
            linkedCards = listOf(linkedCard),
            darkTheme = false,
            onEditAccount = { editCalls++ },
            onUpdateBalance = { updateCalls++ },
            onViewHistory = { historyCalls++ },
            onUnlinkCard = { unlinkId = it },
        )

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithContentDescription("Unlink card").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("History").performClick()

        assertEquals(1, editCalls)
        assertEquals(0, updateCalls)
        assertEquals(1, historyCalls)
        assertEquals(7L, unlinkId)
    }

    @Test
    fun creditCardWithoutLimitDoesNotInventAvailableCredit() {
        setCreditContent(
            card = account(
                bankName = "Penny Card",
                accountLast4 = "9912",
                balance = "12850.25",
                isCreditCard = true,
            ),
            darkTheme = false,
        )

        composeTestRule
            .onNodeWithText("Set a credit limit to see available credit and utilisation")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Available").assertDoesNotExist()
        composeTestRule.onNodeWithText("Credit Limit").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "+night")
    fun hiddenSavingsCardSupportsLargeFontRtlAndAmoled() {
        setAccountContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
                alias = "Primary household account",
                profileId = ProfileEntity.BUSINESS_ID,
            ),
            linkedCards = listOf(
                CardEntity(
                    id = 7,
                    cardLast4 = "7319",
                    cardType = CardType.DEBIT,
                    bankName = "Penny Bank",
                    accountLast4 = "2048",
                ),
            ),
            darkTheme = true,
            isHidden = true,
            amoled = true,
            fontScale = 2f,
            layoutDirection = LayoutDirection.Rtl,
        )

        composeTestRule.onNodeWithText("Primary household account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Business").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Ignored").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun mixedSavingsAndCreditCardsKeepIndependentPresentation() {
        setMixedContent()

        composeTestRule.onNodeWithText("Bank Accounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Credit Cards").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily spending").assertIsDisplayed()
        composeTestRule.onNodeWithText("Penny Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Available").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun hiddenAccountMenuExposesShowCallback() {
        var visibilityCalls = 0
        setAccountContent(
            account = account(
                bankName = "Penny Bank",
                accountLast4 = "2048",
                balance = "42500.75",
            ),
            linkedCards = emptyList(),
            darkTheme = false,
            isHidden = true,
            onToggleVisibility = { visibilityCalls++ },
        )

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Stop ignoring").performClick()

        assertEquals(1, visibilityCalls)
    }

    private fun setAccountContent(
        account: AccountBalanceEntity,
        linkedCards: List<CardEntity>,
        darkTheme: Boolean,
        isHidden: Boolean = false,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onEditAccount: () -> Unit = {},
        onUpdateBalance: () -> Unit = {},
        onViewHistory: () -> Unit = {},
        onUnlinkCard: (Long) -> Unit = {},
        onToggleVisibility: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TestSurface(
                darkTheme = darkTheme,
                amoled = amoled,
                fontScale = fontScale,
                layoutDirection = layoutDirection,
            ) {
                AccountItem(
                    account = account,
                    linkedCards = linkedCards,
                    isHidden = isHidden,
                    onToggleVisibility = onToggleVisibility,
                    onUpdateBalance = onUpdateBalance,
                    onViewHistory = onViewHistory,
                    onUnlinkCard = onUnlinkCard,
                    onEditAccount = onEditAccount,
                )
            }
        }
    }

    private fun setCreditContent(
        card: AccountBalanceEntity,
        darkTheme: Boolean,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            TestSurface(
                darkTheme = darkTheme,
                amoled = amoled,
                fontScale = fontScale,
                layoutDirection = layoutDirection,
            ) {
                CreditCardItem(
                    card = card,
                    isHidden = false,
                    onToggleVisibility = {},
                    onUpdateBalance = {},
                    onViewHistory = {},
                    onDeleteAccount = {},
                )
            }
        }
    }

    private fun setMixedContent() {
        composeTestRule.setContent {
            TestSurface(darkTheme = false) {
                Text(text = "Bank Accounts", style = MaterialTheme.typography.titleMedium)
                AccountItem(
                    account = account(
                        bankName = "Penny Bank",
                        accountLast4 = "2048",
                        balance = "42500.75",
                        alias = "Daily spending",
                    ),
                    isHidden = false,
                    onToggleVisibility = {},
                    onUpdateBalance = {},
                    onViewHistory = {},
                )
                Text(text = "Credit Cards", style = MaterialTheme.typography.titleMedium)
                CreditCardItem(
                    card = account(
                        bankName = "Penny Card",
                        accountLast4 = "9912",
                        balance = "12850.25",
                        creditLimit = "50000",
                        isCreditCard = true,
                    ),
                    isHidden = false,
                    onToggleVisibility = {},
                    onUpdateBalance = {},
                    onViewHistory = {},
                    onDeleteAccount = {},
                )
            }
        }
    }

    private fun account(
        bankName: String,
        accountLast4: String,
        balance: String,
        alias: String? = null,
        sourceType: String? = null,
        profileId: Long = ProfileEntity.PERSONAL_ID,
        creditLimit: String? = null,
        isCreditCard: Boolean = false,
    ) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = accountLast4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
        sourceType = sourceType,
        creditLimit = creditLimit?.let(::BigDecimal),
        isCreditCard = isCreditCard,
        currency = "INR",
        alias = alias,
        profileId = profileId,
    )
}

@Composable
private fun TestSurface(
    darkTheme: Boolean,
    amoled: Boolean = false,
    fontScale: Float = 1f,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density, fontScale),
        LocalLayoutDirection provides layoutDirection,
    ) {
        PennyWiseTheme(
            darkTheme = darkTheme,
            dynamicColor = false,
            themeStyle = ThemeStyle.BRANDED,
            isAmoledMode = amoled,
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
                ) {
                    content()
                }
            }
        }
    }
}
