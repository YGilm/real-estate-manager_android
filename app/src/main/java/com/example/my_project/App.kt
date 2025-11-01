package com.example.my_project

import androidx.compose.runtime.Composable
import com.example.my_project.navigation.RealEstateNavigation
import com.example.my_project.ui.theme.AppTheme

@Composable
fun RealEstateApp() {
    AppTheme {
        RealEstateNavigation()
    }
}