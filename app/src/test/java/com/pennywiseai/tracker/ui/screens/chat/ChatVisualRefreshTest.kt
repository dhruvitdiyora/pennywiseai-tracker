package com.pennywiseai.tracker.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.ChatMessage
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ChatVisualRefreshTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageHierarchyAndComposer_light() {
        setContent(darkTheme = false) { sampleConversation() }

        composeTestRule.onNodeWithText("How much did I spend on dining this month?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "You spent ₹4,280 across six dining transactions. That is 12% less than last month."
        )
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Copy message").assertCountEquals(2)
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun messageHierarchySupportsLargeFont_dark() {
        setContent(darkTheme = true, fontScale = 2f) { sampleConversation() }

        composeTestRule.onNodeWithText("Ask, or tell me what you spent…").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun messageHierarchyPreservesTonalLayers_amoled() {
        setContent(darkTheme = true, amoled = true) { sampleConversation() }

        composeTestRule.onAllNodesWithContentDescription("Copy message").assertCountEquals(2)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun messageHierarchySupportsDynamicColor() {
        setContent(
            darkTheme = false,
            themeStyle = ThemeStyle.DYNAMIC,
            dynamicColor = true,
        ) { sampleConversation() }

        composeTestRule.onAllNodesWithContentDescription("Copy message").assertCountEquals(2)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun messageHierarchyMirrorsInRtl() {
        setContent(
            darkTheme = false,
            layoutDirection = LayoutDirection.Rtl,
        ) { sampleConversation() }

        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun copyIsTheOnlyMessageActionAndInvokesCallback() {
        var copied: ChatMessage? = null
        val assistant = assistantMessage()
        setContent(darkTheme = false) {
            ChatMessageItem(message = assistant, onCopy = { copied = it })
        }

        composeTestRule.onNodeWithContentDescription("Copy message").performClick()

        assertEquals(assistant, copied)
        composeTestRule.onAllNodesWithContentDescription("Edit").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Regenerate").assertCountEquals(0)
    }

    @Test
    fun composerSendsThroughKeyboardAction() {
        var sent = 0
        setContent(darkTheme = false) {
            var value by remember { mutableStateOf("") }
            ChatComposer(
                value = value,
                onValueChange = { value = it },
                onSend = { sent++ },
                enabled = true,
                isLoading = false,
                focusRequester = FocusRequester(),
            )
        }

        composeTestRule.onNodeWithText("Ask, or tell me what you spent…").performTextInput("Show food")
        composeTestRule.onNodeWithText("Show food").performImeAction()

        assertEquals(1, sent)
    }

    @Test
    fun clearRequiresExplicitConfirmation() {
        var clears = 0
        var dismissals = 0
        setContent(darkTheme = false) {
            ChatClearConfirmationDialog(
                onConfirm = { clears++ },
                onDismiss = { dismissals++ },
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, clears)
        assertEquals(1, dismissals)
    }

    @Test
    fun clearConfirmationInvokesDestructiveCallback() {
        var clears = 0
        setContent(darkTheme = false) {
            ChatClearConfirmationDialog(
                onConfirm = { clears++ },
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithText("Clear chat").performClick()
        assertEquals(1, clears)
    }

    @Composable
    private fun sampleConversation() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ChatMessageItem(message = userMessage())
            ChatMessageItem(message = assistantMessage())
            ChatComposer(
                value = "",
                onValueChange = {},
                onSend = {},
                enabled = true,
                isLoading = false,
                focusRequester = FocusRequester(),
            )
        }
    }

    private fun userMessage() = ChatMessage(
        id = "user",
        message = "How much did I spend on dining this month?",
        isUser = true,
        timestamp = 1_700_000_000_000,
    )

    private fun assistantMessage() = ChatMessage(
        id = "assistant",
        message = "You spent ₹4,280 across six dining transactions. That is 12% less than last month.",
        isUser = false,
        timestamp = 1_700_000_060_000,
    )

    private fun setContent(
        darkTheme: Boolean,
        fontScale: Float = 1f,
        amoled: Boolean = false,
        themeStyle: ThemeStyle = ThemeStyle.BRANDED,
        dynamicColor: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale,
                ),
                LocalLayoutDirection provides layoutDirection,
            ) {
                PennyWiseTheme(
                    darkTheme = darkTheme,
                    dynamicColor = dynamicColor,
                    themeStyle = themeStyle,
                    isAmoledMode = amoled,
                    blurEffects = false,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
