package com.pennywiseai.tracker.ui.screens.onboarding

import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.manager.SmsScanManager
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf

/**
 * Focused UI coverage for the existing five-stage onboarding flow.
 *
 * The test deliberately drives the public screen with an explicit ViewModel;
 * no ViewModel behavior is asserted. Fixtures use the existing public stage
 * transitions and an in-memory account row, so tests do not start a real scan
 * or persist onboarding data.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, sdk = [35])
class OnBoardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: PennyWiseDatabase
    private lateinit var viewModel: OnBoardingViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ensureWorkManager(context)
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = OnBoardingViewModel(
            userPreferencesRepository = UserPreferencesRepository(context),
            smsScanManager = SmsScanManager(context),
            accountBalanceRepository = AccountBalanceRepository(
                accountBalanceDao = database.accountBalanceDao(),
                transactionDao = database.transactionDao(),
                database = database,
            ),
            context = context,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun welcome_light() {
        setContent()

        composeTestRule.onNodeWithText("Welcome to PennyWise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun welcome_largeFont() {
        setContent(fontScale = 1.3f)

        composeTestRule.onNodeWithText("Welcome to PennyWise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun profile_dark() {
        viewModel.navigateToStep(OnBoardingStep.PROFILE)
        viewModel.updateUserName("A")
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Make PennyWise yours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose an avatar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pick a background colour").assertExists()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun permissions_dark() {
        viewModel.navigateToStep(OnBoardingStep.PERMISSIONS)
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Enable automatic detection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your privacy matters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable permissions").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun scan_stage_dark() {
        viewModel.navigateToStep(OnBoardingStep.SMS_SCAN)
        setContent(darkTheme = true)

        composeTestRule.onNodeWithText("Scan Your Messages").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Scanning").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun account_selection_light() {
        seedAccounts()
        viewModel.navigateToStep(OnBoardingStep.ACCOUNT_SETUP)
        viewModel.loadAccounts()
        setContent()

        waitUntilAsyncWorkCompletes { viewModel.uiState.value.accounts.isNotEmpty() }
        viewModel.selectAccount("Primary Bank_2468")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select your main account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Primary Bank").assertIsDisplayed()
        composeTestRule.onNodeWithText("••••2468").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reserve Bank").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun account_empty_light() {
        viewModel.navigateToStep(OnBoardingStep.ACCOUNT_SETUP)
        setContent()

        composeTestRule.onNodeWithText("You’re all set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finish").assertIsEnabled()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun progressAndBack_followTheExistingStageOrder() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Step 1 of 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.onNodeWithContentDescription("Step 2 of 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Make PennyWise yours").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Welcome to PennyWise").assertIsDisplayed()
    }

    @Test
    fun reducedMotion_navigationReplacesTheStageWithoutDelay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val previousScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        try {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            setContent()

            composeTestRule.onNodeWithText("Get Started").performClick()
            composeTestRule.onNodeWithText("Make PennyWise yours").assertIsDisplayed()
            composeTestRule.onNodeWithText("Welcome to PennyWise").assertDoesNotExist()
        } finally {
            Settings.Global.putFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                previousScale,
            )
        }
    }

    @Test
    fun profileContinue_requiresAName_andAdvances() {
        viewModel.navigateToStep(OnBoardingStep.PROFILE)
        setContent()

        composeTestRule.onNodeWithText("Save & Continue").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Your name").performTextInput("A")
        composeTestRule.onNodeWithText("Save & Continue").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("Enable automatic detection").assertIsDisplayed()
    }

    @Test
    fun scanActions_exposeStartAndSkip_beforeScanning() {
        viewModel.navigateToStep(OnBoardingStep.SMS_SCAN)
        setContent()

        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Scanning").assertIsDisplayed()
    }

    @Test
    fun grantedPermissions_continue_reachesScanStage() {
        viewModel.onSmsPermissionResult(true)
        viewModel.navigateToStep(OnBoardingStep.PERMISSIONS)
        setContent()

        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Scan Your Messages").assertIsDisplayed()
    }

    @Test
    fun permissionSkip_reachesAccountSetup_withoutPermissionPrompt() {
        viewModel.navigateToStep(OnBoardingStep.PERMISSIONS)
        setContent()

        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("You’re all set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable permissions").assertDoesNotExist()
    }

    @Test
    fun accountSelection_emitsSelection_andFinishCompletes() {
        seedAccounts()
        viewModel.navigateToStep(OnBoardingStep.ACCOUNT_SETUP)
        viewModel.loadAccounts()
        var completed = false
        setContent(onComplete = { completed = true })

        waitUntilAsyncWorkCompletes { viewModel.uiState.value.accounts.isNotEmpty() }
        composeTestRule.onNodeWithText("Finish").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Primary Bank").performClick()
        composeTestRule.onNodeWithContentDescription("Selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finish").performClick()
        waitUntilAsyncWorkCompletes { completed }
        assertTrue(completed)
    }

    @Test
    fun emptyAccountFinish_completesWithoutAnAccount() {
        viewModel.navigateToStep(OnBoardingStep.ACCOUNT_SETUP)
        var completed = false
        setContent(onComplete = { completed = true })

        composeTestRule.onNodeWithText("Finish").performClick()
        waitUntilAsyncWorkCompletes { completed }
        assertTrue(completed)
    }

    private fun setContent(
        darkTheme: Boolean = false,
        onComplete: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            PennyWiseTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                themeStyle = ThemeStyle.BRANDED,
                blurEffects = false,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        OnBoardingScreen(
                            onOnboardingComplete = onComplete,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }

    private fun seedAccounts() = runBlocking {
        database.accountBalanceDao().insertBalance(account("Primary Bank", "2468", "12500.00"))
        database.accountBalanceDao().insertBalance(account("Reserve Bank", "1357", "8000.00"))
    }

    private fun waitUntilAsyncWorkCompletes(condition: () -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            // ViewModel work posts back to the Robolectric main looper after Room/DataStore IO.
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun account(bankName: String, last4: String, balance: String) = AccountBalanceEntity(
        bankName = bankName,
        accountLast4 = last4,
        balance = BigDecimal(balance),
        timestamp = LocalDateTime.of(2026, 9, 1, 10, 0),
        accountType = "SAVINGS",
        currency = "INR",
    )

    private fun ensureWorkManager(context: Context) {
        runCatching { WorkManager.getInstance(context) }.getOrElse {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
    }
}
