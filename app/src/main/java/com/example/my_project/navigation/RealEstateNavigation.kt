@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.my_project.ui.AuthViewModel
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.screens.AddPropertyScreen
import com.example.my_project.ui.screens.EditPropertyScreen
import com.example.my_project.ui.screens.HomeScreen
import com.example.my_project.ui.screens.PropertiesListScreen
import com.example.my_project.ui.screens.PropertyDetailsScreen
import com.example.my_project.ui.screens.PropertyTransactionsScreen
import com.example.my_project.ui.screens.SignInScreen
import com.example.my_project.ui.screens.SignUpScreen
import com.example.my_project.ui.screens.StatsMonthScreen
import com.example.my_project.ui.screens.StatsScreen

/* ---------- Навигационные маршруты ---------- */
sealed class Destinations(val route: String) {

    data object AuthSignIn : Destinations("auth_signin")
    data object AuthSignUp : Destinations("auth_signup")

    data object Home : Destinations("home")

    data object Properties : Destinations("properties")
    data object AddProperty : Destinations("add_property")

    data object PropertyDetails : Destinations("property_details/{id}") {
        fun route(id: String) = "property_details/$id"
    }

    data object EditProperty : Destinations("edit_property/{id}") {
        fun route(id: String) = "edit_property/$id"
    }

    data object PropertyTransactions : Destinations("property_transactions/{id}") {
        fun route(id: String) = "property_transactions/$id"
    }

    /** Общая статистика (опциональный параметр propertyId) */
    data object Stats : Destinations("stats?propertyId={propertyId}") {
        fun route(propertyId: String? = null): String =
            if (propertyId.isNullOrBlank()) "stats" else "stats?propertyId=$propertyId"
    }

    /** Детализация по месяцу (опционально propertyId как query) */
    data object StatsMonth : Destinations("stats_month/{year}/{month}?propertyId={propertyId}") {
        fun route(year: Int, month: Int, propertyId: String? = null): String {
            val base = "stats_month/$year/$month"
            return if (propertyId.isNullOrBlank()) base else "$base?propertyId=$propertyId"
        }
    }
}

/* ---------- Главная функция навигации ---------- */
@Composable
fun RealEstateNavigation() {
    val nav = rememberNavController()

    // ViewModel'ы
    val authVm: AuthViewModel = hiltViewModel()
    val mainVm: RealEstateViewModel = hiltViewModel()

    // Текущий пользователь
    val userIdState by authVm.userId.collectAsState()

    // Стартовый экран
    val startDestination =
        if (userIdState == null) Destinations.AuthSignIn.route else Destinations.Home.route

    NavHost(
        navController = nav,
        startDestination = startDestination
    ) {
        /* ---- Авторизация ---- */
        composable(Destinations.AuthSignIn.route) {
            SignInScreen(
                onSignIn = { email, pass, remember ->
                    authVm.login(email, pass, remember) { err ->
                        if (err == null) {
                            nav.navigate(Destinations.Home.route) { popUpTo(0) }
                        }
                    }
                },
                onGoSignUp = { nav.navigate(Destinations.AuthSignUp.route) }
            )
        }

        composable(Destinations.AuthSignUp.route) {
            SignUpScreen(
                onSignUp = { email, pass, remember ->
                    authVm.register(email, pass, remember) { err ->
                        if (err == null) {
                            nav.navigate(Destinations.Home.route) { popUpTo(0) }
                        }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        /* ---- Главный экран ---- */
        composable(Destinations.Home.route) {
            HomeScreen(
                onOpenProperties = { nav.navigate(Destinations.Properties.route) },
                onOpenStats = { nav.navigate(Destinations.Stats.route()) }
            )
        }

        /* ---- Список объектов ---- */
        composable(Destinations.Properties.route) {
            PropertiesListScreen(
                vm = mainVm,
                onAdd = { nav.navigate(Destinations.AddProperty.route) },
                onOpen = { id -> nav.navigate(Destinations.PropertyDetails.route(id)) },
                onBack = { nav.popBackStack() }
            )
        }

        /* ---- Добавление объекта ---- */
        composable(Destinations.AddProperty.route) {
            AddPropertyScreen(
                onSave = { name, address, rent ->
                    mainVm.addProperty(name, address, rent)
                    nav.popBackStack()
                },
                onCancel = { nav.popBackStack() }
            )
        }

        /* ---- Детали объекта ---- */
        composable(
            route = Destinations.PropertyDetails.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable

            PropertyDetailsScreen(
                vm = mainVm,
                propertyId = id,
                onBack = { nav.popBackStack() },
                onEditProperty = { nav.navigate(Destinations.EditProperty.route(id)) },
                onOpenStatsForProperty = {
                    nav.navigate(Destinations.Stats.route(propertyId = id))
                },
                onOpenBills = {
                    // 👇 Отсюда идём в список счетов по объекту
                    nav.navigate(BillsDest.list(id))
                },
                onOpenTransactions = {
                    nav.navigate(Destinations.PropertyTransactions.route(id))
                }
            )
        }

        /* ---- Редактирование объекта ---- */
        composable(
            route = Destinations.EditProperty.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            EditPropertyScreen(
                vm = mainVm,
                propertyId = id,
                onBack = { nav.popBackStack() }
            )
        }

        /* ---- Транзакции по объекту ---- */
        composable(
            route = Destinations.PropertyTransactions.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            PropertyTransactionsScreen(
                vm = mainVm,
                propertyId = id,
                onBack = { nav.popBackStack() }
            )
        }

        /* ---- Общая статистика ---- */
        composable(
            route = Destinations.Stats.route,
            arguments = listOf(
                navArgument("propertyId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            // пока StatsScreen не принимает propertyId, просто игнорируем аргумент
            StatsScreen(
                vm = mainVm,
                onBack = { nav.popBackStack() },
                onOpenMonth = { year, month, pid ->
                    nav.navigate(Destinations.StatsMonth.route(year, month, pid))
                }
            )
        }

        /* ---- Статистика по месяцу ---- */
        composable(
            route = Destinations.StatsMonth.route,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType },
                navArgument("propertyId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: return@composable
            val month = backStackEntry.arguments?.getInt("month") ?: return@composable
            val propertyIdArg =
                backStackEntry.arguments?.getString("propertyId")?.takeIf { it.isNotBlank() }

            StatsMonthScreen(
                vm = mainVm,
                year = year,
                month = month,
                propertyId = propertyIdArg,
                onBack = { nav.popBackStack() }
            )
        }
    }
}