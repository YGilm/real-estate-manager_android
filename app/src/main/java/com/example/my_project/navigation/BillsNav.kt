package com.example.my_project.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.bills.BillEditScreen
import com.example.my_project.ui.bills.BillsListScreen

/**
 * Маршруты для раздела "Счета".
 */
object BillsDest {

    // Паттерны для NavHost
    const val LIST_PATTERN = "bills/{propertyId}"
    const val EDIT_PATTERN = "bills/{propertyId}/edit?billId={billId}"

    // Готовые строки для навигации
    fun list(propertyId: String): String = "bills/$propertyId"

    fun new(propertyId: String): String = "bills/$propertyId/edit"

    fun edit(propertyId: String, billId: String): String =
        "bills/$propertyId/edit?billId=$billId"
}

/**
 * Вложенный навграф для работы со счетами.
 */
fun NavGraphBuilder.registerBillsGraph(
    navController: NavHostController,
    vm: RealEstateViewModel // сейчас не используем, но оставляем на будущее
) {
    // ----- Список счетов -----
    composable(
        route = BillsDest.LIST_PATTERN,
        arguments = listOf(
            navArgument("propertyId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val propertyId = backStackEntry.arguments?.getString("propertyId") ?: return@composable

        BillsListScreen(
            propertyId = propertyId,
            onBack = { navController.popBackStack() },
            onOpenBill = { billId ->
                val route = if (billId == null) {
                    BillsDest.new(propertyId)
                } else {
                    BillsDest.edit(propertyId, billId)
                }
                navController.navigate(route)
            }
        )
    }

    // ----- Создание / редактирование одного счёта -----
    composable(
        route = BillsDest.EDIT_PATTERN,
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
            propertyId = propertyId,
            billId = billId,
            onBack = { navController.popBackStack() }
        )
    }
}