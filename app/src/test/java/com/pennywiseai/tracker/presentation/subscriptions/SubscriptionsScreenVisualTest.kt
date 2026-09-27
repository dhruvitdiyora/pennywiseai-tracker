package com.pennywiseai.tracker.presentation.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SubscriptionsScreenVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun item_statuses_light() {
        setContent(darkTheme = false)

        composeTestRule.onNodeWithText("Overdue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paid", substring = true).assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun item_statuses_dark() {
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Overdue").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun startToEndSwipe_opensEditDialog() {
        var editRequested = false
        setContent(
            darkTheme = false,
            onEditRequested = { editRequested = true },
        )

        composeTestRule
            .onNodeWithTag("subscription_item_1")
            .performTouchInput { swipeRight() }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { editRequested }
        assertTrue(editRequested)
    }

    @Test
    fun endToStartSwipe_requestsHide() {
        var hideRequested = false
        setContent(
            darkTheme = false,
            onHide = { hideRequested = true },
        )

        composeTestRule
            .onNodeWithTag("subscription_item_1")
            .performTouchInput { swipeLeft() }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { hideRequested }
        assertTrue(hideRequested)
    }

    private fun setContent(
        darkTheme: Boolean,
        onEditRequested: (() -> Unit)? = null,
        onHide: () -> Unit = {},
    ) {
        val today = LocalDate.of(2026, 9, 3)
        val subscriptions = listOf(
            subscription(
                id = 1,
                merchantName = "Streaming service",
                nextPaymentDate = today.minusDays(1),
            ),
            subscription(
                id = 2,
                merchantName = "Cloud storage",
                nextPaymentDate = today.minusDays(3),
                lastPaidAt = today.minusDays(1),
            ),
            subscription(
                id = 3,
                merchantName = "News subscription",
                nextPaymentDate = today.plusDays(2),
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
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        subscriptions.forEach { subscription ->
                            SwipeableSubscriptionItem(
                                subscription = subscription,
                                isPaidThisCycle = subscription.lastPaidAt != null,
                                today = today,
                                onEditRequested = onEditRequested,
                                onHide = onHide,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun subscription(
        id: Long,
        merchantName: String,
        nextPaymentDate: LocalDate,
        lastPaidAt: LocalDate? = null,
    ) = SubscriptionEntity(
        id = id,
        merchantName = merchantName,
        amount = BigDecimal("499"),
        nextPaymentDate = nextPaymentDate,
        lastPaidAt = lastPaidAt,
    )
}
