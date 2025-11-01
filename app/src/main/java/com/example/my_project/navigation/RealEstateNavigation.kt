package com.example.my_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.my_project.ui.screens.*

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

    data object Stats : Destinations("stats")
}

@Composable
fun RealEstateNavigation() {
    val nav = rememberNavController()

    val authVm: AuthViewModel = hiltViewModel()
    val mainVm: RealEstateViewModel = hiltViewModel()

    val userIdState by authVm.userId.collectAsState()
    val start = if (userIdState == null) Destinations.AuthSignIn.route else Destinations.Home.route

    // Реактивный переход при логине/логауте
    LaunchedEffect(userIdState) {
        val current = nav.currentDestination?.route
        if (userIdState == null && current != Destinations.AuthSignIn.route) {
            nav.navigate(Destinations.AuthSignIn.route) { popUpTo(0) }
        }
        if (userIdState != null && (current == Destinations.AuthSignIn.route || current == Destinations.AuthSignUp.route)) {
            nav.navigate(Destinations.Home.route) { popUpTo(0) }
        }
    }

    NavHost(navController = nav, startDestination = start) {

        // SignIn
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

        // SignUp
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

        // Home
        composable(Destinations.Home.route) {
            HomeScreen(
                onOpenStats = { nav.navigate(Destinations.Stats.route) },
                onOpenProperties = { nav.navigate(Destinations.Properties.route) }
                // Если добавишь кнопку выхода на Home, просто вызови authVm.logout() внутри экрана
            )
        }

        // Properties
        composable(Destinations.Properties.route) {
            PropertiesListScreen(
                vm = mainVm,
                onAdd = { nav.navigate(Destinations.AddProperty.route) },
                onOpen = { id -> nav.navigate(Destinations.PropertyDetails.route(id)) },
                onBack = { nav.popBackStack() }
            )
        }

        // Add property
        composable(Destinations.AddProperty.route) {
            AddPropertyScreen(
                onSave = { name, address, rent ->
                    mainVm.addProperty(name, address, rent)
                    nav.popBackStack()
                },
                onCancel = { nav.popBackStack() }
            )
        }

        // Property details
        composable(
            route = Destinations.PropertyDetails.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            PropertyDetailsScreen(
                vm = mainVm,
                propertyId = id,
                onBack = { nav.popBackStack() },
                onEditProperty = { nav.navigate(Destinations.EditProperty.route(id)) }
            )
        }

        // Edit property
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

        // Stats
        composable(Destinations.Stats.route) {
            StatsScreen(
                vm = mainVm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}