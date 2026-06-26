package com.example.real_estate_manager.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.real_estate_manager.BuildConfig
import com.example.real_estate_manager.ui.AuthViewModel
import com.example.real_estate_manager.ui.RealEstateViewModel
import com.example.real_estate_manager.ui.ServerSettingsViewModel
import com.example.real_estate_manager.ui.screens.AddPropertyScreen
import com.example.real_estate_manager.ui.screens.EditPropertyScreen
import com.example.real_estate_manager.ui.screens.HomeScreen
import com.example.real_estate_manager.ui.screens.NotificationDetailsScreen
import com.example.real_estate_manager.ui.screens.LockScreen
import com.example.real_estate_manager.ui.screens.NotificationsScreen
import com.example.real_estate_manager.ui.screens.PropertiesListScreen
import com.example.real_estate_manager.ui.screens.PropertyDetailsScreen
import com.example.real_estate_manager.ui.screens.PropertyInfoScreen
import com.example.real_estate_manager.ui.screens.PropertyReadingsScreen
import com.example.real_estate_manager.ui.screens.PropertyTransactionsScreen
import com.example.real_estate_manager.ui.screens.RemindersScreen
import com.example.real_estate_manager.ui.screens.ServerSettingsScreen
import com.example.real_estate_manager.ui.screens.SignInScreen
import com.example.real_estate_manager.ui.screens.SignUpScreen
import com.example.real_estate_manager.ui.screens.StatsMonthScreen
import com.example.real_estate_manager.ui.screens.StatsScreen

sealed class Destination(val route: String) {

    data object Gate : Destination("gate")

    data object SignIn : Destination("auth/signin")
    data object SignUp : Destination("auth/signup")
    data object Lock : Destination("auth/lock")

    data object Home : Destination("home")
    data object ServerSettings : Destination("settings/server")
    data object Notifications : Destination("notifications")

    data object NotificationDetails : Destination("notifications/{notificationId}") {
        const val ARG_NOTIFICATION_ID = "notificationId"
        fun route(notificationId: String): String = "notifications/${Uri.encode(notificationId)}"
    }
    data object Properties : Destination("properties")
    data object AddProperty : Destination("properties/add")

    data object PropertyDetails : Destination("properties/details/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun route(propertyId: String): String = "properties/details/${Uri.encode(propertyId)}"
    }

    data object PropertyInfo : Destination("properties/info/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun route(propertyId: String): String = "properties/info/${Uri.encode(propertyId)}"
    }

    data object EditProperty : Destination("properties/edit/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun route(propertyId: String): String = "properties/edit/${Uri.encode(propertyId)}"
    }

    data object PropertyTransactions : Destination("properties/{propertyId}/transactions") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun route(propertyId: String): String = "properties/${Uri.encode(propertyId)}/transactions"
    }

    data object PropertyReadings : Destination("properties/{propertyId}/readings") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun route(propertyId: String): String = "properties/${Uri.encode(propertyId)}/readings"
    }

    data object Reminders : Destination("reminders?propertyId={propertyId}&reminderId={reminderId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        const val ARG_REMINDER_ID = "reminderId"

        fun route(propertyId: String? = null, reminderId: String? = null): String {
            val params = buildList {
                propertyId?.takeIf { it.isNotBlank() }?.let { add("propertyId=${Uri.encode(it)}") }
                reminderId?.takeIf { it.isNotBlank() }?.let { add("reminderId=${Uri.encode(it)}") }
            }
            return if (params.isEmpty()) "reminders" else "reminders?${params.joinToString("&")}"
        }
    }

    data object Stats : Destination("stats?propertyId={propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"

        fun route(propertyId: String? = null): String {
            val id = propertyId?.takeIf { it.isNotBlank() }
            return if (id == null) "stats" else "stats?propertyId=${Uri.encode(id)}"
        }
    }

    data object StatsMonth : Destination("stats/month/{year}/{month}?propertyId={propertyId}") {
        const val ARG_YEAR = "year"
        const val ARG_MONTH = "month"
        const val ARG_PROPERTY_ID = "propertyId"

        fun route(year: Int, month: Int, propertyId: String?): String {
            val id = propertyId?.takeIf { it.isNotBlank() }
            return if (id == null) {
                "stats/month/$year/$month"
            } else {
                "stats/month/$year/$month?propertyId=${Uri.encode(id)}"
            }
        }
    }
}

@Composable
fun RealEstateNavigation(
    notificationNavigationTarget: NotificationNavigationTarget? = null,
    onNotificationNavigationHandled: () -> Unit = {}
) {
    val navController: NavHostController = rememberNavController()

    val vm: RealEstateViewModel = hiltViewModel()
    val authVm: AuthViewModel = hiltViewModel()
    val serverSettingsVm: ServerSettingsViewModel = hiltViewModel()

    val sessionState by authVm.sessionState.collectAsState()
    val serverSettingsState by serverSettingsVm.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var pendingNotificationTarget by remember { mutableStateOf<NotificationNavigationTarget?>(null) }

    fun notificationRoute(target: NotificationNavigationTarget): String =
        target.notificationId
            ?.takeIf { it.isNotBlank() }
            ?.let { Destination.NotificationDetails.route(it) }
            ?: Destination.Notifications.route

    fun consumeNotificationTarget() {
        pendingNotificationTarget = null
        onNotificationNavigationHandled()
    }

    LaunchedEffect(Unit) {
        serverSettingsVm.startupCheck()
    }
    LaunchedEffect(notificationNavigationTarget) {
        if (notificationNavigationTarget != null) {
            pendingNotificationTarget = notificationNavigationTarget
        }
    }
    // Если разлогинились — мгновенно уводим на SignIn и чистим backstack
    LaunchedEffect(sessionState.userId, currentRoute) {
        val preAuthRoute = currentRoute in setOf(
            Destination.Gate.route,
            Destination.SignIn.route,
            Destination.SignUp.route,
            Destination.ServerSettings.route
        )
        if (sessionState.userId == null && !preAuthRoute) {
            val startId = navController.graph.findStartDestination().id
            navController.navigate(Destination.SignIn.route) {
                popUpTo(startId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(serverSettingsState.connectionStatus, currentRoute) {
        val serverUnavailable = serverSettingsState.connectionStatus != "Не проверено" &&
            !serverSettingsState.connectionStatus.contains("найден", ignoreCase = true)
        val startupRoute = currentRoute in setOf(
            Destination.Gate.route,
            Destination.SignIn.route,
            Destination.SignUp.route
        )
        if (serverUnavailable && startupRoute) {
            val startId = navController.graph.findStartDestination().id
            navController.navigate(Destination.ServerSettings.route) {
                popUpTo(startId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(serverSettingsState.connectionStatus, sessionState.userId) {
        if (serverSettingsState.connectionStatus.startsWith("Автоматически найден сервер") &&
            sessionState.userId != null
        ) {
            vm.refreshCloudData()
        }
    }

    LaunchedEffect(pendingNotificationTarget, sessionState.userId, sessionState.locked, currentRoute) {
        val target = pendingNotificationTarget ?: notificationNavigationTarget
        if (target != null &&
            sessionState.userId != null &&
            !sessionState.locked &&
            currentRoute != null &&
            currentRoute != Destination.Gate.route
        ) {
            navController.navigate(notificationRoute(target)) {
                launchSingleTop = true
            }
            consumeNotificationTarget()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Gate.route
    ) {

        composable(Destination.Gate.route) {
            LaunchedEffect(sessionState.userId, sessionState.locked, pendingNotificationTarget, notificationNavigationTarget) {
                val startId = navController.graph.findStartDestination().id
                if (sessionState.userId == null) {
                    navController.navigate(Destination.SignIn.route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (sessionState.locked) {
                    navController.navigate(Destination.Lock.route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    val target = pendingNotificationTarget ?: notificationNavigationTarget
                    val route = if (target != null) {
                        notificationRoute(target)
                    } else {
                        Destination.Home.route
                    }
                    navController.navigate(route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (target != null) {
                        consumeNotificationTarget()
                    }
                }
            }
        }

        // -------- AUTH --------

        composable(Destination.SignIn.route) {
            LaunchedEffect(sessionState.userId, sessionState.locked) {
                if (sessionState.userId != null) {
                    val startId = navController.graph.findStartDestination().id
                    val target = pendingNotificationTarget ?: notificationNavigationTarget
                    val route = when {
                        sessionState.locked -> Destination.Lock.route
                        target != null -> notificationRoute(target)
                        else -> Destination.Home.route
                    }
                    navController.navigate(route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (!sessionState.locked && target != null) {
                        consumeNotificationTarget()
                    }
                }
            }
            SignInScreen(
                onSignIn = { email, pass, remember, onDone ->
                    authVm.login(email, pass, remember) { err ->
                        onDone(err)
                        if (err == null) {
                            val startId = navController.graph.findStartDestination().id
                            val target = pendingNotificationTarget ?: notificationNavigationTarget
                            navController.navigate(target?.let(::notificationRoute) ?: Destination.Home.route) {
                                popUpTo(startId) { inclusive = true }
                                launchSingleTop = true
                            }
                            if (target != null) {
                                consumeNotificationTarget()
                            }
                        }
                    }
                },
                onGoSignUp = { navController.navigate(Destination.SignUp.route) },
                onOpenServerSettings = { navController.navigate(Destination.ServerSettings.route) },
                serverStatus = serverSettingsState.connectionStatus
            )
        }

        composable(Destination.SignUp.route) {
            SignUpScreen(
                onSignUp = { email, pass, remember, onDone ->
                    authVm.register(email, pass, remember) { err ->
                        onDone(err)
                        if (err == null) {
                            val startId = navController.graph.findStartDestination().id
                            navController.navigate(Destination.Home.route) {
                                popUpTo(startId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destination.Lock.route) {
            val email by authVm.currentEmail.collectAsState()
            LockScreen(
                email = email,
                onUnlockByBiometricSuccess = {
                    authVm.unlockByBiometricSuccess()
                    val startId = navController.graph.findStartDestination().id
                    val target = pendingNotificationTarget ?: notificationNavigationTarget
                    navController.navigate(target?.let(::notificationRoute) ?: Destination.Home.route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (target != null) {
                        consumeNotificationTarget()
                    }
                },
                onUnlockByPassword = { pass, onDone ->
                    authVm.unlockByPassword(pass) { err ->
                        onDone(err)
                        if (err == null) {
                            val startId = navController.graph.findStartDestination().id
                            val target = pendingNotificationTarget ?: notificationNavigationTarget
                            navController.navigate(target?.let(::notificationRoute) ?: Destination.Home.route) {
                                popUpTo(startId) { inclusive = true }
                                launchSingleTop = true
                            }
                            if (target != null) {
                                consumeNotificationTarget()
                            }
                        }
                    }
                },
                onLogout = {
                    authVm.logout()
                    val startId = navController.graph.findStartDestination().id
                    navController.navigate(Destination.SignIn.route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // -------- APP --------

        composable(Destination.Home.route) {
            HomeScreen(
                onOpenStats = { navController.navigate(Destination.Stats.route()) },
                onOpenProperties = { navController.navigate(Destination.Properties.route) },
                onOpenNotifications = { navController.navigate(Destination.Notifications.route) },
                onOpenServerSettings = { navController.navigate(Destination.ServerSettings.route) },
                showDeveloperTools = BuildConfig.DEBUG,
                serverWarning = serverSettingsState.connectionStatus,
                onLogoutNavigate = {
                    val startId = navController.graph.findStartDestination().id
                    navController.navigate(Destination.SignIn.route) {
                        popUpTo(startId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Destination.ServerSettings.route) {
            ServerSettingsScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destination.SignIn.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Destination.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenNotification = { id -> navController.navigate(Destination.NotificationDetails.route(id)) },
                onOpenRelated = { type, id, propertyId -> navController.navigateRelated(type, id, propertyId) }
            )
        }

        composable(
            route = Destination.NotificationDetails.route,
            arguments = listOf(
                navArgument(Destination.NotificationDetails.ARG_NOTIFICATION_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments
                ?.getString(Destination.NotificationDetails.ARG_NOTIFICATION_ID)
                ?: return@composable

            NotificationDetailsScreen(
                notificationId = notificationId,
                onBack = { navController.popBackStack() },
                onOpenRelated = { type, id, propertyId -> navController.navigateRelated(type, id, propertyId) }
            )
        }

        composable(Destination.Properties.route) {
            PropertiesListScreen(
                vm = vm,
                onAdd = { navController.navigate(Destination.AddProperty.route) },
                onOpen = { propertyId ->
                    navController.navigate(Destination.PropertyDetails.route(propertyId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destination.AddProperty.route) {
            AddPropertyScreen(
                onSave = { name, address, monthlyRent, areaSqm, leaseFrom, leaseTo, coverUri ->
                    vm.addProperty(name, address, monthlyRent, areaSqm, leaseFrom, leaseTo, coverUri)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.PropertyDetails.route,
            arguments = listOf(
                navArgument(Destination.PropertyDetails.ARG_PROPERTY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(Destination.PropertyDetails.ARG_PROPERTY_ID)
                ?: return@composable

            PropertyDetailsScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onEditProperty = { navController.navigate(Destination.EditProperty.route(propertyId)) },
                onOpenDetails = { navController.navigate(Destination.PropertyInfo.route(propertyId)) },
                onOpenStatsForProperty = { navController.navigate(Destination.Stats.route(propertyId)) },
                onOpenBills = { navController.navigate(Destination.PropertyReadings.route(propertyId)) },
                onOpenTransactions = { navController.navigate(Destination.PropertyTransactions.route(propertyId)) },
                onOpenReminders = { navController.navigate(Destination.Reminders.route(propertyId)) }
            )
        }

        composable(
            route = Destination.PropertyInfo.route,
            arguments = listOf(
                navArgument(Destination.PropertyInfo.ARG_PROPERTY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(Destination.PropertyInfo.ARG_PROPERTY_ID)
                ?: return@composable

            PropertyInfoScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.EditProperty.route,
            arguments = listOf(
                navArgument(Destination.EditProperty.ARG_PROPERTY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(Destination.EditProperty.ARG_PROPERTY_ID)
                ?: return@composable

            EditPropertyScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onDeleted = {
                    navController.navigate(Destination.Properties.route) {
                        popUpTo(Destination.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Destination.PropertyTransactions.route,
            arguments = listOf(
                navArgument(Destination.PropertyTransactions.ARG_PROPERTY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(Destination.PropertyTransactions.ARG_PROPERTY_ID)
                ?: return@composable

            PropertyTransactionsScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.PropertyReadings.route,
            arguments = listOf(
                navArgument(Destination.PropertyReadings.ARG_PROPERTY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(Destination.PropertyReadings.ARG_PROPERTY_ID)
                ?: return@composable

            PropertyReadingsScreen(
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.Reminders.route,
            arguments = listOf(
                navArgument(Destination.Reminders.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Destination.Reminders.ARG_REMINDER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            RemindersScreen(
                propertyId = backStackEntry.arguments?.getString(Destination.Reminders.ARG_PROPERTY_ID),
                focusReminderId = backStackEntry.arguments?.getString(Destination.Reminders.ARG_REMINDER_ID),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.Stats.route,
            arguments = listOf(
                navArgument(Destination.Stats.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(Destination.Stats.ARG_PROPERTY_ID)

            StatsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                preselectedPropertyId = propertyId,
                onOpenMonth = { year, month, pid ->
                    navController.navigate(Destination.StatsMonth.route(year, month, pid))
                }
            )
        }

        composable(
            route = Destination.StatsMonth.route,
            arguments = listOf(
                navArgument(Destination.StatsMonth.ARG_YEAR) { type = NavType.IntType },
                navArgument(Destination.StatsMonth.ARG_MONTH) { type = NavType.IntType },
                navArgument(Destination.StatsMonth.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt(Destination.StatsMonth.ARG_YEAR) ?: return@composable
            val month = backStackEntry.arguments?.getInt(Destination.StatsMonth.ARG_MONTH) ?: return@composable
            val propertyId = backStackEntry.arguments?.getString(Destination.StatsMonth.ARG_PROPERTY_ID)

            StatsMonthScreen(
                vm = vm,
                year = year,
                month = month,
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun NavHostController.navigateRelated(type: String?, id: String?, propertyId: String?) {
    when (type) {
        "REMINDER" -> navigate(Destination.Reminders.route(propertyId, id))
        "PROPERTY" -> id?.takeIf { it.isNotBlank() }?.let { navigate(Destination.PropertyDetails.route(it)) }
        "PAYMENT" -> propertyId?.takeIf { it.isNotBlank() }?.let { navigate(Destination.PropertyTransactions.route(it)) }
        else -> navigate(Destination.Notifications.route)
    }
}
