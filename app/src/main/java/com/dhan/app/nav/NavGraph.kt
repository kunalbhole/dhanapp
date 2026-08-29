package com.dhan.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhan.app.ui.screens.auth.ForgotPinScreen
import com.dhan.app.ui.screens.auth.IncomeSetupScreen
import com.dhan.app.ui.screens.auth.LinkBankScreen
import com.dhan.app.ui.screens.auth.LoginScreen
import com.dhan.app.ui.screens.auth.PermissionsScreen
import com.dhan.app.ui.screens.auth.SignUpScreen
import com.dhan.app.ui.screens.detail.BillDetailScreen
import com.dhan.app.ui.screens.detail.EditBudgetScreen
import com.dhan.app.ui.screens.detail.EmptyStateScreen
import com.dhan.app.ui.screens.detail.FriendDetailScreen
import com.dhan.app.ui.screens.detail.GoalsScreen
import com.dhan.app.ui.screens.detail.HelpScreen
import com.dhan.app.ui.screens.detail.InsightsScreen
import com.dhan.app.ui.screens.detail.LinkedAccountsScreen
import com.dhan.app.ui.screens.detail.NotificationsScreen
import com.dhan.app.ui.screens.detail.PlusPaywallScreen
import com.dhan.app.ui.screens.detail.ProfileEditScreen
import com.dhan.app.ui.screens.detail.TxnDetailScreen
import com.dhan.app.ui.screens.main.BillsScreen
import com.dhan.app.ui.screens.main.BudgetScreen
import com.dhan.app.ui.screens.main.DebtScreen
import com.dhan.app.ui.screens.main.HomeScreen
import com.dhan.app.ui.screens.main.SettingsScreen
import com.dhan.app.ui.screens.main.TransactionsScreen
import com.dhan.app.ui.screens.onboarding.FrameworkScreen
import com.dhan.app.ui.screens.onboarding.OnboardingScreen
import com.dhan.app.ui.screens.onboarding.SplashScreen

@Composable
fun DhanNavHost(startDestination: String) {
    val navController: NavHostController = rememberNavController()
    val actions = remember(navController) { DhanNavActions(navController) }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SPLASH) { SplashScreen(actions) }
        composable(Routes.ONBOARDING) { OnboardingScreen(actions) }
        composable(Routes.SIGNUP) { SignUpScreen(actions) }
        composable(Routes.PERMISSIONS) { PermissionsScreen(actions) }
        composable(Routes.LINKBANK) { LinkBankScreen(actions) }
        composable(Routes.INCOME) { IncomeSetupScreen(actions) }
        composable(Routes.FRAMEWORK) { FrameworkScreen(actions) }
        composable(Routes.LOGIN) { LoginScreen(actions) }
        composable(Routes.FORGOT_PIN) { ForgotPinScreen(actions) }

        composable(Routes.HOME) { HomeScreen(actions) }
        composable(Routes.TXN) { TransactionsScreen(actions) }
        composable(Routes.BUDGET) { BudgetScreen(actions) }
        composable(Routes.BILLS) { BillsScreen(actions) }
        composable(Routes.DEBT) { DebtScreen(actions) }
        composable(Routes.SETTINGS) { SettingsScreen(actions) }

        composable(
            Routes.TXN_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            TxnDetailScreen(actions, backStackEntry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.EDIT_BUDGET) { EditBudgetScreen(actions) }
        composable(
            Routes.BILL_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            BillDetailScreen(actions, backStackEntry.arguments?.getLong("id") ?: 0L)
        }
        composable(
            Routes.FRIEND,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            FriendDetailScreen(actions, backStackEntry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.INSIGHTS) { InsightsScreen(actions) }
        composable(Routes.GOALS) { GoalsScreen(actions) }
        composable(Routes.PROFILE) { ProfileEditScreen(actions) }
        composable(Routes.LINKED) { LinkedAccountsScreen(actions) }
        composable(Routes.HELP) { HelpScreen(actions) }
        composable(Routes.PAYWALL) { PlusPaywallScreen(actions) }
        composable(Routes.EMPTY) { EmptyStateScreen(actions) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(actions) }
    }
}
