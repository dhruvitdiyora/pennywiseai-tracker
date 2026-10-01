package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.pennywiseai.tracker.data.database.entity.LoanDirection
import com.pennywiseai.tracker.data.database.entity.LoanEntity
import com.pennywiseai.tracker.data.database.entity.PersonEntity
import com.pennywiseai.tracker.data.preferences.ThemeStyle
import com.pennywiseai.tracker.data.repository.PersonLoanSummary
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class PeopleScreensVisualTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun contacts_currencySeparated_light() {
        var selectedPersonId: Long? = null
        var editedPersonId: Long? = null
        setContent(darkTheme = false) {
            ContactsScreenContent(
                state = PeopleUiState(people = people(), isLoading = false),
                onQueryChanged = {},
                onNavigateBack = {},
                onAddPerson = {},
                onEditPerson = { editedPersonId = it.id },
                onPersonClick = { selectedPersonId = it.person.id },
            )
        }

        composeTestRule.onNodeWithText("You get ${money("250", "INR")}").assertIsDisplayed()
        composeTestRule.onNodeWithText("You owe ${money("15", "USD")}").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()

        composeTestRule.onNodeWithText("Household contact").performClick()
        composeTestRule.onNodeWithContentDescription("Edit Work contact").performClick()
        assertEquals(1L, selectedPersonId)
        assertEquals(2L, editedPersonId)
    }

    @Test
    @Config(qualifiers = "+night")
    fun personalDashboard_currencySeparated_dark() {
        var contactsRequested = false
        var loansRequested = false
        var selectedPersonId: Long? = null
        setContent(darkTheme = true) {
            PersonalDashboardContent(
                state = PersonalDashboardUiState(
                    userName = "App user",
                    people = people(),
                    loanSummary = PersonLoanSummary(
                        personId = -1,
                        personName = "",
                        lentByCurrency = mapOf("INR" to BigDecimal("800")),
                        borrowedByCurrency = mapOf("USD" to BigDecimal("25")),
                        netByCurrency = mapOf(
                            "INR" to BigDecimal("800"),
                            "USD" to BigDecimal("-25"),
                        ),
                        activeLoanCount = 2,
                    ),
                    isLoading = false,
                ),
                onNavigateBack = {},
                onNavigateToContacts = { contactsRequested = true },
                onNavigateToLoans = { loansRequested = true },
                onNavigateToPerson = { selectedPersonId = it },
            )
        }

        // The banner header pushes the lower sections below the first screen, and a
        // lazy list only composes what is on screen: capture the top, then scroll to
        // each section before asserting on it.
        composeTestRule.onRoot().captureRoboImage()

        scrollDashboardTo("INR")
        composeTestRule.onNodeWithText("INR").assertIsDisplayed()
        composeTestRule.onNodeWithText("USD").assertIsDisplayed()
        composeTestRule.onNodeWithText(money("800", "INR")).assertIsDisplayed()
        composeTestRule.onNodeWithText(money("25", "USD")).assertIsDisplayed()

        scrollDashboardTo("Contacts")
        composeTestRule.onNodeWithText("Contacts").performClick()
        composeTestRule.onNodeWithText("Lend & Borrow").performClick()
        scrollDashboardTo("Household contact")
        composeTestRule.onNodeWithText("Household contact").performClick()
        assertTrue(contactsRequested)
        assertTrue(loansRequested)
        assertEquals(1L, selectedPersonId)
    }

    @Test
    fun personDetail_showsCurrencyTaggedBalancesAndRecord() {
        var addRequested = false
        var selectedLoanId: Long? = null
        val person = people().first().person
        setContent(darkTheme = false) {
            PersonDetailScreenContent(
                state = PersonDetailUiState(
                    person = person,
                    loans = listOf(
                        LoanEntity(
                            id = 7,
                            personName = person.name,
                            personId = person.id,
                            direction = LoanDirection.LENT,
                            originalAmount = BigDecimal("100"),
                            remainingAmount = BigDecimal("100"),
                            currency = "INR",
                            note = "Shared expense",
                            createdAt = LocalDateTime.of(2026, 9, 1, 12, 0),
                            updatedAt = LocalDateTime.of(2026, 9, 1, 12, 0),
                        ),
                    ),
                    summary = PersonLoanSummary(
                        personId = person.id,
                        personName = person.name,
                        lentByCurrency = mapOf("INR" to BigDecimal("600")),
                        borrowedByCurrency = mapOf("USD" to BigDecimal("25")),
                        netByCurrency = mapOf(
                            "INR" to BigDecimal("600"),
                            "USD" to BigDecimal("-25"),
                        ),
                        activeLoanCount = 2,
                    ),
                    isLoading = false,
                ),
                onNavigateBack = {},
                onNavigateToLoan = { selectedLoanId = it },
                onEdit = {},
                onAddEntry = { addRequested = true },
                onDeleteOrArchive = {},
            )
        }

        composeTestRule.onNodeWithText("Add record", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText(money("600", "INR")).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(money("25", "USD")).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(money("100", "INR")).performScrollTo().performClick()
        assertTrue(addRequested)
        assertEquals(7L, selectedLoanId)
    }

    private fun people(): List<PersonWithSummary> = listOf(
        PersonWithSummary(
            person = PersonEntity(
                id = 1,
                name = "Household contact",
                normalizedName = "household contact",
                category = "Family",
                color = "#4CAF50",
            ),
            summary = PersonLoanSummary(
                personId = 1,
                personName = "Household contact",
                lentByCurrency = mapOf("INR" to BigDecimal("250")),
                borrowedByCurrency = mapOf("USD" to BigDecimal("15")),
                netByCurrency = mapOf(
                    "INR" to BigDecimal("250"),
                    "USD" to BigDecimal("-15"),
                ),
                activeLoanCount = 2,
            ),
        ),
        PersonWithSummary(
            person = PersonEntity(
                id = 2,
                name = "Work contact",
                normalizedName = "work contact",
                category = "Work",
                color = "#2196F3",
            ),
            summary = PersonLoanSummary(
                personId = 2,
                personName = "Work contact",
            ),
        ),
    )

    private fun money(amount: String, currency: String): String =
        CurrencyFormatter.formatCurrency(BigDecimal(amount), currency)

    /** Scrolls the dashboard's outer list (the first scrollable; the contact carousel nests inside it) to [text]. */
    private fun scrollDashboardTo(text: String) {
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text))
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
                    content()
                }
            }
        }
    }
}
