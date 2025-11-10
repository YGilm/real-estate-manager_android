package com.example.my_project.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.bills.BillEditScreen
import com.example.my_project.ui.bills.BillsListScreen

/**
 * Маршруты для блока "Счета" внутри объекта недвижимости.
 */
object BillsDest {
    const val LIST = "bills/{propertyId}"
    fun list(propertyId: String) = "bills/$propertyId"

    const val EDIT = "bills/{propertyId}/edit?billId={billId}"
    fun edit(propertyId: String, billId: String? = null): String =
        if (billId == null) "bills/$propertyId/edit"
        else "bills/$propertyId/edit?billId=$billId"
}

fun NavGraphBuilder.registerBillsGraph(
    navController: NavController,
    vm: RealEstateViewModel
) {
    // Список счетов для объекта
    composable(
        route = BillsDest.LIST,
        arguments = listOf(
            navArgument("propertyId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val propertyId = backStackEntry.arguments?.getString("propertyId") ?: return@composable

        BillsListScreen(
            vm = vm,
            propertyId = propertyId,
            onBack = { navController.popBackStack() },
            onCreate = { navController.navigate(BillsDest.edit(propertyId)) },
            onEdit = { billId -> navController.navigate(BillsDest.edit(propertyId, billId)) }
        )
    }

    // Создание/редактирование счёта
    composable(
        route = BillsDest.EDIT,
        arguments = listOf(
            navArgument("propertyId") { type = NavType.StringType },
            navArgument("billId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val propertyId = backStackEntry.arguments?.getString("propertyId") ?: return@composable
        val billId = backStackEntry.arguments?.getString("billId")

        BillEditScreen(
            vm = vm,
            propertyId = propertyId,
            billId = billId,
            onBack = { navController.popBackStack() }
        )
    }
}