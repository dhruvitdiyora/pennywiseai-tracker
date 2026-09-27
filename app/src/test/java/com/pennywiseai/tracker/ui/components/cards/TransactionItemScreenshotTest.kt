package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class TransactionItemScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun grouped_light() {
        setContent(darkTheme = false)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun grouped_dark() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun convertedAmountShowsDisplayAndNativeCurrencies() {
        val transaction = transaction(
            id = 4,
            merchant = "Overseas purchase",
            category = "Shopping",
            amount = "100",
            type = TransactionType.EXPENSE,
            currency = "USD",
        )
        setSingleContent(
            transaction = transaction,
            convertedAmount = BigDecimal("8300"),
            displayCurrency = "INR",
        )

        composeTestRule.onNodeWithText(
            "-${CurrencyFormatter.formatCurrency(BigDecimal("8300"), "INR")}",
        ).assertExists()
        composeTestRule.onNodeWithText(
            "(${CurrencyFormatter.formatCurrency(BigDecimal("100"), "USD")})",
        ).assertExists()
    }

    @Test
    fun missingConversionKeepsNativeCurrency() {
        val transaction = transaction(
            id = 5,
            merchant = "Overseas purchase",
            category = "Shopping",
            amount = "100",
            type = TransactionType.EXPENSE,
            currency = "USD",
        )
        setSingleContent(transaction = transaction)

        composeTestRule.onNodeWithText(
            "-${CurrencyFormatter.formatCurrency(BigDecimal("100"), "USD")}",
        ).assertExists()
        composeTestRule.onNodeWithText(
            "-${CurrencyFormatter.formatCurrency(BigDecimal("100"), "INR")}",
        ).assertDoesNotExist()
    }

    @Test
    fun clippedTagsExposeCompleteAccessibleMetadata() {
        val transaction = transaction(
            id = 6,
            merchant = "Fresh Market",
            category = "Food",
            amount = "1840.50",
            type = TransactionType.EXPENSE,
            description = "Weekly groceries",
            balanceAfter = "78159.50",
            isRecurring = true,
            profileId = ProfileEntity.BUSINESS_ID,
            excludedFromAnalytics = true,
        )
        setSingleContent(transaction = transaction)

        val date = transaction.dateTime.format(DateTimeFormatter.ofPattern("d MMM · h:mm a"))
        val expected = listOf(
            date,
            "Food",
            "Recurring",
            "Business",
            "Excluded",
            "Bal ${CurrencyFormatter.formatCurrency(BigDecimal("78159.50"), "INR")}",
            "Weekly groceries",
        ).joinToString(" · ")
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    private fun setContent(darkTheme: Boolean) {
        val transactions = listOf(
            transaction(
                id = 1,
                merchant = "Fresh Market",
                category = "Food",
                amount = "1840.50",
                type = TransactionType.EXPENSE,
                description = "Weekly groceries",
                balanceAfter = "78159.50",
            ),
            transaction(
                id = 2,
                merchant = "Acme Payroll",
                category = "Income",
                amount = "85000",
                type = TransactionType.INCOME,
                isRecurring = true,
                profileId = ProfileEntity.BUSINESS_ID,
            ),
            transaction(
                id = 3,
                merchant = "Self transfer",
                category = "Transfer",
                amount = "12000",
                type = TransactionType.TRANSFER,
                accountNumber = "1234",
                fromAccount = "1234",
                toAccount = "9912",
            ),
        )

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
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Layout.groupedListGap),
                    ) {
                        transactions.forEachIndexed { index, transaction ->
                            TransactionItem(
                                transaction = transaction,
                                listItemPosition = ListItemPosition.from(index, transactions.size),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setSingleContent(
        transaction: TransactionEntity,
        convertedAmount: BigDecimal? = null,
        displayCurrency: String? = null,
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
                    Column(modifier = Modifier.padding(Dimensions.Padding.content)) {
                        TransactionItem(
                            transaction = transaction,
                            convertedAmount = convertedAmount,
                            displayCurrency = displayCurrency,
                        )
                    }
                }
            }
        }
    }

    private fun transaction(
        id: Long,
        merchant: String,
        category: String,
        amount: String,
        type: TransactionType,
        description: String? = null,
        balanceAfter: String? = null,
        isRecurring: Boolean = false,
        excludedFromAnalytics: Boolean = false,
        profileId: Long? = null,
        accountNumber: String? = null,
        fromAccount: String? = null,
        toAccount: String? = null,
        currency: String = "INR",
    ) = TransactionEntity(
        id = id,
        amount = BigDecimal(amount),
        merchantName = merchant,
        category = category,
        transactionType = type,
        dateTime = LocalDateTime.of(2026, 9, id.toInt(), 10 + id.toInt(), 15),
        description = description,
        accountNumber = accountNumber,
        balanceAfter = balanceAfter?.let(::BigDecimal),
        transactionHash = "screenshot-$id",
        isRecurring = isRecurring,
        excludedFromAnalytics = excludedFromAnalytics,
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount,
        profileId = profileId,
    )
}
