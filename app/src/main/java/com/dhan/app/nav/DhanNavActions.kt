package com.dhan.app.nav

import androidx.navigation.NavHostController
import com.dhan.app.ui.components.DhanTab

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SIGNUP = "signup"
    const val PERMISSIONS = "permissions"
    const val LINKBANK = "linkbank"
    const val INCOME = "income"
    const val FRAMEWORK = "framework"
    const val LOGIN = "login"
    const val FORGOT_PIN = "forgot_pin"
    const val HOME = "home"
    const val TXN = "txn"
    const val TXN_DETAIL = "txn_detail/{id}"
    const val BUDGET = "budget"
    const val EDIT_BUDGET = "edit_budget"
    const val BILLS = "bills"
    const val BILL_DETAIL = "bill_detail/{id}"
    const val DEBT = "debt"
    const val FRIEND = "friend/{id}"
    const val INSIGHTS = "insights"
    const val GOALS = "goals"
    const val PROFILE = "profile"
    const val LINKED = "linked"
    const val HELP = "help"
    const val PAYWALL = "paywall"
    const val EMPTY = "empty"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    fun txnDetail(id: Long) = "txn_detail/$id"
    fun billDetail(id: Long) = "bill_detail/$id"
    fun friend(id: Long) = "friend/$id"
}

/** All screen-to-screen transitions the ported screens call into, mirroring app.jsx's
 *  `nav()`, `onTab()`, and each screen's individual `onDone` callback. Screens should
 *  never call `navController.navigate(...)` directly — go through this so back-stack
 *  behavior (auth flow vs. tab switch vs. detail push) stays consistent in one place. */
class DhanNavActions(private val nav: NavHostController) {
    fun back() = nav.popBackStack()

    // Auth / onboarding flow — forward-only, each step replaces the last so back
    // doesn't re-enter a completed step.
    fun toOnboarding() = nav.navigate(Routes.ONBOARDING)
    fun toSignup() = nav.navigate(Routes.SIGNUP)
    fun toPermissions() = nav.navigate(Routes.PERMISSIONS)
    fun toLinkBank() = nav.navigate(Routes.LINKBANK)
    fun toIncome() = nav.navigate(Routes.INCOME)
    fun toFramework() = nav.navigate(Routes.FRAMEWORK)
    fun toLogin() = nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
    fun toForgotPin() = nav.navigate(Routes.FORGOT_PIN)

    /** Onboarding/login complete -> clear the whole auth back-stack so the hardware
     *  back button from Home exits the app instead of re-entering signup. */
    fun toHomeFresh() = nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }

    /** Bottom-tab switch: preserves each tab's own back-stack/scroll position. */
    fun toTab(tab: DhanTab) {
        val route = if (tab == DhanTab.MORE) Routes.SETTINGS else tab.route
        nav.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun toTxnDetail(id: Long) = nav.navigate(Routes.txnDetail(id))
    fun toEditBudget() = nav.navigate(Routes.EDIT_BUDGET)
    fun toBillDetail(id: Long) = nav.navigate(Routes.billDetail(id))
    fun toDebt() = nav.navigate(Routes.DEBT)
    fun toFriend(id: Long) = nav.navigate(Routes.friend(id))
    fun toInsights() = nav.navigate(Routes.INSIGHTS)
    fun toGoals() = nav.navigate(Routes.GOALS)
    fun toProfile() = nav.navigate(Routes.PROFILE)
    fun toLinked() = nav.navigate(Routes.LINKED)
    fun toHelp() = nav.navigate(Routes.HELP)
    fun toPaywall() = nav.navigate(Routes.PAYWALL)
    fun toEmpty() = nav.navigate(Routes.EMPTY)
    fun toSettings() = nav.navigate(Routes.SETTINGS)
    fun toNotifications() = nav.navigate(Routes.NOTIFICATIONS)

    /** Sign-out: wipe the whole back-stack back to Splash (mirrors app.jsx clearing
     *  localStorage and returning to "splash"). */
    fun signOutToSplash() = nav.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
}
