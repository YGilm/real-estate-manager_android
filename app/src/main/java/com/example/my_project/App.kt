@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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