package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ReceiptAttachmentCardVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun receiptCard_light() {
        var shareRequested = false
        setContent(darkTheme = false) {
            ReceiptAttachmentCard(
                model = android.R.drawable.ic_menu_gallery,
                onView = {},
                onShare = { shareRequested = true },
            )
        }

        composeTestRule.onRoot().captureRoboImage()
        composeTestRule.onNodeWithContentDescription("Share receipt").performClick()
        assertTrue(shareRequested)
    }

    @Test
    @Config(qualifiers = "+night")
    fun receiptCard_dark() {
        setContent(darkTheme = true) {
            ReceiptAttachmentCard(
                model = android.R.drawable.ic_menu_gallery,
                onView = {},
                onShare = {},
            )
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    private fun setContent(
        darkTheme: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
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
                        content()
                    }
                }
            }
        }
    }
}
