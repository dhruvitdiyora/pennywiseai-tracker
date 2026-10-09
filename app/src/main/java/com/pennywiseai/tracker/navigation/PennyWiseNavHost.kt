package com.pennywiseai.tracker.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.pennywiseai.tracker.ui.LocalNavAnimatedVisibilityScope
import com.pennywiseai.tracker.ui.LocalSharedTransitionScope
import com.pennywiseai.tracker.ui.MainScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

/**
 * Safe version of popBackStack that prevents rapid back presses from causing
 * screen overlap. Only pops if the current entry is fully RESUMED.
 */
fun NavHostController.safePopBackStack() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

@Composable
fun PennyWiseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }

    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
    NavHost(
        navController = navController,
        startDestination = stableStartDestination,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
    ) {
        composable<AppLock>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.AppLockScreen(
                onUnlocked = {
                    navController.navigate(Home) {
                        launchSingleTop = true
                        popUpTo(AppLock) { inclusive = true }
                    }
                }
            )
        }
        composable<OnBoarding>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.onboarding.OnBoardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Home) {
                        launchSingleTop = true
                        popUpTo(OnBoarding) { inclusive = true }
                    }
                }
            )
        }
        composable<Home>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                MainScreen(
                    rootNavController = navController
                )
            }
        }

        composable<Settings>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.SettingsScreen(
                themeViewModel = themeViewModel,
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToCategories = {
                    navController.navigate(Categories) { launchSingleTop = true }
                },
                onNavigateToFaq = {
                    navController.navigate(Faq) { launchSingleTop = true }
                },
                onNavigateToBudgets = {
                    navController.navigate(BudgetGroups) { launchSingleTop = true }
                },
                onNavigateToTransactionGroups = {
                    navController.navigate(TransactionGroups) { launchSingleTop = true }
                },
                onNavigateToPersonalDashboard = {
                    navController.navigate(PersonalDashboard) { launchSingleTop = true }
                },
                onNavigateToCurrencyFormats = {
                    navController.navigate(SettingsCurrencyFormats) { launchSingleTop = true }
                },
                onNavigateToBackupImport = {
                    navController.navigate(SettingsBackupImport) { launchSingleTop = true }
                },
                onNavigateToPrivacySecurity = {
                    navController.navigate(SettingsPrivacySecurity) { launchSingleTop = true }
                },
                onNavigateToAdvanced = {
                    navController.navigate(SettingsAdvanced) { launchSingleTop = true }
                },
                onNavigateToManageAccounts = {
                    navController.navigate(ManageAccounts) { launchSingleTop = true }
                },
                onNavigateToRules = {
                    navController.navigate(Rules) { launchSingleTop = true }
                },
                onNavigateToLoans = {
                    navController.navigate(Loans) { launchSingleTop = true }
                },
                onNavigateToRecurring = {
                    navController.navigate(RecurringTransactions) { launchSingleTop = true }
                },
                onNavigateToAppearance = {
                    navController.navigate(SettingsAppearance) { launchSingleTop = true }
                },
                onNavigateToProfiles = {
                    navController.navigate(SettingsProfiles) { launchSingleTop = true }
                },
                onNavigateToAbout = {
                    navController.navigate(SettingsAbout) { launchSingleTop = true }
                },
            )
        }

        composable<SettingsCurrencyFormats>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.CurrencyFormatsSettingsScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToExchangeRates = {
                    navController.navigate(ExchangeRates) { launchSingleTop = true }
                },
            )
        }

        composable<SettingsBackupImport>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.BackupImportSettingsScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToImportStatement = {
                    navController.navigate(ImportStatement) { launchSingleTop = true }
                },
                onNavigateToUnrecognizedSms = {
                    navController.navigate(UnrecognizedSms) { launchSingleTop = true }
                },
            )
        }

        composable<SettingsPrivacySecurity>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.PrivacySecuritySettingsScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToDataPrivacy = {
                    navController.navigate(SettingsDataPrivacy) { launchSingleTop = true }
                },
            )
        }

        composable<SettingsDataPrivacy>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.DataPrivacyScreen(
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        composable<SettingsAppearance>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.AppearanceScreen(
                onNavigateBack = { navController.safePopBackStack() },
                themeViewModel = themeViewModel,
            )
        }

        composable<SettingsProfiles>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.ProfileScreen(
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        composable<SettingsAbout>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.AboutScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToLicenses = {
                    navController.navigate(SettingsLicenses) { launchSingleTop = true }
                },
            )
        }

        composable<SettingsLicenses>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.LicensesScreen(
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        composable<ManageAccounts>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.accounts.ManageAccountsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToAddAccount = {
                    navController.navigate(AddAccount) { launchSingleTop = true }
                },
                onNavigateToBalanceHistory = { bankName, accountLast4 ->
                    navController.navigate(BalanceHistory(bankName, accountLast4)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAccountDetail = { bankName, accountLast4 ->
                    navController.navigate(AccountDetail(bankName, accountLast4)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AddAccount>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.accounts.AddAccountScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        // BalanceHistoryViewModel reads "bankName" / "accountLast4" from its
        // SavedStateHandle; the typed route's property names supply them.
        composable<BalanceHistory>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.accounts.BalanceHistoryScreen(
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        composable<SettingsAdvanced>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.AdvancedSettingsScreen(
                onNavigateBack = { navController.safePopBackStack() },
            )
        }

        composable<Categories>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.categories.CategoriesScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }
        
        composable<TransactionDetail>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) { backStackEntry ->
            val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                com.pennywiseai.tracker.presentation.transactions.TransactionDetailScreen(
                    transactionId = transactionDetail.transactionId,
                    onNavigateBack = {
                        onEditComplete()
                        navController.safePopBackStack()
                    },
                    onNavigateToLoanDetail = { loanId ->
                        navController.navigate(LoanDetail(loanId)) {
                            launchSingleTop = true
                        }
                    },
                    onDuplicateTransaction = { sourceId ->
                        navController.navigate(AddTransaction(sourceTransactionId = sourceId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable<AddTransaction>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.add.AddScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }
        
        composable<UnrecognizedSms>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.unrecognized.UnrecognizedSmsScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }
        
        composable<Faq>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.settings.FAQScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        composable<Rules>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.ui.screens.rules.RulesScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToCreateRule = {
                    navController.navigate(CreateRule()) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditRule = { ruleId ->
                    navController.navigate(CreateRule(ruleId = ruleId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDuplicateRule = { ruleId ->
                    navController.navigate(CreateRule(duplicateFromId = ruleId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<CreateRule>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) { backStackEntry ->
            val createRule = backStackEntry.toRoute<CreateRule>()
            val rulesViewModel: com.pennywiseai.tracker.ui.viewmodel.RulesViewModel = hiltViewModel()

            // Collect rules from the flow to find the rule being edited or duplicated.
            val rules by rulesViewModel.rules.collectAsStateWithLifecycle()
            val isEditing = createRule.ruleId != null

            // Stable id for a duplicate, kept across recompositions/config changes.
            val duplicateId = rememberSaveable { java.util.UUID.randomUUID().toString() }

            // Edit prefills from the saved rule; duplicate prefills from the source but
            // becomes a fresh rule (new id, "(copy)" name) so saving inserts a new one.
            val prefillRule = when {
                createRule.ruleId != null ->
                    rules.firstOrNull { it.id == createRule.ruleId }
                createRule.duplicateFromId != null ->
                    rules.firstOrNull { it.id == createRule.duplicateFromId }?.let { source ->
                        source.copy(
                            id = duplicateId,
                            name = "${source.name} (copy)",
                            isSystemTemplate = false
                        )
                    }
                else -> null
            }
            val accounts by rulesViewModel.accounts.collectAsStateWithLifecycle()
            val categories by rulesViewModel.categories.collectAsStateWithLifecycle()

            com.pennywiseai.tracker.ui.screens.rules.CreateRuleScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onSaveRule = { rule ->
                    // Edit updates in place; create/duplicate inserts a new rule.
                    if (isEditing) {
                        rulesViewModel.updateRule(rule)
                    } else {
                        rulesViewModel.createRule(rule)
                    }
                    navController.safePopBackStack()
                },
                existingRule = prefillRule,
                isEditing = isEditing,
                allAccounts = accounts,
                categories = categories
            )
        }
        
        composable<AccountDetail>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.accounts.AccountDetailScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(TransactionDetail(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<BudgetGroups>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.budgetgroups.BudgetGroupsScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToGroupEdit = { groupId ->
                    navController.navigate(BudgetGroupEdit(groupId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = { groupId, year, month ->
                    navController.navigate(BudgetHistory(groupId, year, month)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDetail = { groupId, year, month ->
                    navController.navigate(BudgetDetail(groupId, year, month)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCategory = { category, yearMonth, currency ->
                    navController.navigate(TransactionsWithFilter(category, yearMonth, currency)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<BudgetDetail>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<BudgetDetail>()
            val budgetGroupsEntry = remember(backStackEntry) {
                navController.getBackStackEntry(BudgetGroups)
            }
            val budgetGroupsViewModel:
                com.pennywiseai.tracker.presentation.budgetgroups.BudgetGroupsViewModel =
                hiltViewModel(budgetGroupsEntry)
            com.pennywiseai.tracker.presentation.budgetgroups.BudgetDetailScreen(
                groupId = args.groupId,
                year = args.year,
                month = args.month,
                viewModel = budgetGroupsViewModel,
                onNavigateBack = { navController.safePopBackStack() },
                onEdit = { groupId ->
                    navController.navigate(BudgetGroupEdit(groupId)) {
                        launchSingleTop = true
                    }
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionDetail(transactionId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = { groupId, year, month ->
                    navController.navigate(BudgetHistory(groupId, year, month)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<BudgetGroupEdit>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.budgetgroups.BudgetGroupEditScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        composable<BudgetHistory>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.budgetgroups.BudgetHistoryScreen(
                onNavigateBack = { navController.safePopBackStack() }
            )
        }

        composable<Loans>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.loans.LoansScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToLoanDetail = { loanId ->
                    navController.navigate(LoanDetail(loanId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToContacts = {
                    navController.navigate(Contacts) { launchSingleTop = true }
                },
            )
        }

        composable<Contacts>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.people.ContactsScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToPerson = { personId ->
                    navController.navigate(PersonDetail(personId)) { launchSingleTop = true }
                },
            )
        }

        composable<PersonalDashboard>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.people.PersonalDashboardScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToContacts = {
                    navController.navigate(Contacts) { launchSingleTop = true }
                },
                onNavigateToLoans = {
                    navController.navigate(Loans) { launchSingleTop = true }
                },
                onNavigateToPerson = { personId ->
                    navController.navigate(PersonDetail(personId)) { launchSingleTop = true }
                },
            )
        }

        composable<PersonDetail>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.people.PersonDetailScreen(
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToLoan = { loanId ->
                    navController.navigate(LoanDetail(loanId)) { launchSingleTop = true }
                },
            )
        }

        composable<RecurringTransactions>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.recurring.RecurringTransactionsScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        composable<LoanDetail>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.loans.LoanDetailScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToTransactionDetail = { txId ->
                    navController.navigate(TransactionDetail(txId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<TransactionGroups>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.groups.TransactionGroupsScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate(TransactionGroupDetail(groupId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<TransactionGroupDetail>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.groups.TransactionGroupDetailScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                },
                onNavigateToTransactionDetail = { txId ->
                    navController.navigate(TransactionDetail(txId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<ExchangeRates>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.exchangerates.ExchangeRatesScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        composable<ImportStatement>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) {
            com.pennywiseai.tracker.presentation.statement.ImportStatementScreen(
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        composable<TransactionsWithFilter>(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutVertically { it / 4 } }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<TransactionsWithFilter>()
            com.pennywiseai.tracker.presentation.transactions.TransactionsScreen(
                initialCategory = args.category,
                initialPeriod = args.period,
                initialCurrency = args.currency,
                onNavigateBack = { navController.safePopBackStack() },
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionDetail(transactionId)) { launchSingleTop = true }
                },
                onAddTransactionClick = {
                    navController.navigate(AddTransaction()) { launchSingleTop = true }
                },
                onNavigateToSettings = {
                    navController.navigate(Settings) { launchSingleTop = true }
                }
            )
        }

    }
    }
    }
}
