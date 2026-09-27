package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class AccountUpdateSheetVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun balance_sheet_light() {
        setBalanceContent(darkTheme = false)

        composeTestRule.onNodeWithText("Update balance").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun credit_limit_step_dark() {
        setCreditContent(darkTheme = true)
        composeTestRule.onNodeWithText("Next").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Step 2 of 2").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun number_pad_edits_balance_and_confirms_exact_value() {
        var confirmed: BigDecimal? = null
        setBalanceContent(darkTheme = false, onConfirm = { confirmed = it })

        composeTestRule.onNodeWithContentDescription("Digit 7").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Digit 3").performClick()
        composeTestRule.onNodeWithContentDescription("Evaluate expression").performClick()
        composeTestRule.onNodeWithText("Update").performScrollTo().performClick()

        assertEquals(BigDecimal.TEN, confirmed)
    }

    @Test
    fun credit_flow_blocks_negative_limit_and_preserves_negative_outstanding() {
        var confirmed: Pair<BigDecimal, BigDecimal>? = null
        setCreditContent(
            darkTheme = false,
            onConfirm = { outstanding, limit -> confirmed = outstanding to limit },
        )
        composeTestRule.onNodeWithText("Next").performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Clear input").performClick()
        composeTestRule.onNodeWithContentDescription("Subtract").performClick()
        composeTestRule.onNodeWithContentDescription("Digit 1").performClick()
        composeTestRule.onNodeWithText("Update").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithContentDescription("Clear input").performClick()
        composeTestRule.onNodeWithContentDescription("Digit 2").performClick()
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Digit 0").performClick()
        }
        composeTestRule.onNodeWithText("Update").performScrollTo().performClick()

        assertEquals(BigDecimal("-150.25"), confirmed?.first)
        assertEquals(BigDecimal("2000"), confirmed?.second)
    }

    @Test
    @Config(qualifiers = "+night")
    fun credit_limit_step_supportsLargeFontRtlAmoledAndBackNavigation() {
        setCreditContent(
            darkTheme = true,
            amoled = true,
            fontScale = 2f,
            layoutDirection = LayoutDirection.Rtl,
        )

        composeTestRule.onNodeWithText("Next").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Step 2 of 2").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Step 1 of 2").assertIsDisplayed()
    }

    private fun setBalanceContent(
        darkTheme: Boolean,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onConfirm: (BigDecimal) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TestTheme(
                darkTheme = darkTheme,
                amoled = amoled,
                fontScale = fontScale,
                layoutDirection = layoutDirection,
            ) {
                UpdateBalanceDialog(
                    bankName = "Sample Bank",
                    accountLast4 = "4242",
                    currentBalance = BigDecimal("125.50"),
                    currencyCode = "USD",
                    onDismiss = {},
                    onConfirm = onConfirm,
                )
            }
        }
    }

    private fun setCreditContent(
        darkTheme: Boolean,
        amoled: Boolean = false,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onConfirm: (BigDecimal, BigDecimal) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            TestTheme(
                darkTheme = darkTheme,
                amoled = amoled,
                fontScale = fontScale,
                layoutDirection = layoutDirection,
            ) {
                UpdateCreditCardDialog(
                    bankName = "Sample Bank",
                    accountLast4 = "4242",
                    currentOutstanding = BigDecimal("-150.25"),
                    currentLimit = BigDecimal("5000"),
                    currencyCode = "USD",
                    onDismiss = {},
                    onConfirm = onConfirm,
                )
            }
        }
    }

    @Composable
    private fun TestTheme(
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}
