package com.example.my_project.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.screens.AddPropertyScreen
import com.example.my_project.ui.screens.EditPropertyScreen
import com.example.my_project.ui.screens.HomeScreen
import com.example.my_project.ui.screens.PropertiesListScreen
import com.example.my_project.ui.screens.PropertyDetailsScreen
import com.example.my_project.ui.screens.PropertyInfoScreen
import com.example.my_project.ui.screens.PropertyTransactionsScreen
import com.example.my_project.ui.screens.StatsMonthScreen
import com.example.my_project.ui.screens.StatsScreen

sealed class Destination(val route: String) {

    data object Home : Destination("home")
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
fun RealEstateNavigation(startDestination: String = Destination.Home.route) {
    val navController = rememberNavController()
    val vm: RealEstateViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Destination.Home.route) {
            HomeScreen(
                onOpenStats = { navController.navigate(Destination.Stats.route()) },
                onOpenProperties = { navController.navigate(Destination.Properties.route) }
            )
        }

        composable(Destination.Properties.route) {
            PropertiesListScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Destination.AddProperty.route) },
                onOpen = { propertyId -> navController.navigate(Destination.PropertyDetails.route(propertyId)) }
            )
        }

        composable(Destination.AddProperty.route) {
            AddPropertyScreen(
                onSave = { name, address, monthlyRent, leaseFrom, leaseTo, coverUri ->
                    vm.addProperty(name, address, monthlyRent, leaseFrom, leaseTo, coverUri)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.PropertyDetails.route,
            arguments = listOf(navArgument(Destination.PropertyDetails.ARG_PROPERTY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(Destination.PropertyDetails.ARG_PROPERTY_ID) ?: return@composable

            PropertyDetailsScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() },
                onEditProperty = { navController.navigate(Destination.EditProperty.route(propertyId)) },

                // ✅ НОВОЕ: кнопка “Детали”
                onOpenDetails = { navController.navigate(Destination.PropertyInfo.route(propertyId)) },

                onOpenStatsForProperty = { navController.navigate(Destination.Stats.route(propertyId)) },
                onOpenBills = { /* заглушка/котик у тебя в UI */ },
                onOpenTransactions = { navController.navigate(Destination.PropertyTransactions.route(propertyId)) }
            )
        }

        composable(
            route = Destination.PropertyInfo.route,
            arguments = listOf(navArgument(Destination.PropertyInfo.ARG_PROPERTY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(Destination.PropertyInfo.ARG_PROPERTY_ID) ?: return@composable

            PropertyInfoScreen(
                vm = vm,
                propertyId = propertyId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.EditProperty.route,
            arguments = listOf(navArgument(Destination.EditProperty.ARG_PROPERTY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(Destination.EditProperty.ARG_PROPERTY_ID) ?: return@composable

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
            arguments = listOf(navArgument(Destination.PropertyTransactions.ARG_PROPERTY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(Destination.PropertyTransactions.ARG_PROPERTY_ID) ?: return@composable

            PropertyTransactionsScreen(
                vm = vm,
                propertyId = propertyId,
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