@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.real_estate_manager

import androidx.compose.runtime.Composable
import com.example.real_estate_manager.navigation.RealEstateNavigation
import com.example.real_estate_manager.ui.theme.AppTheme

@Composable
fun RealEstateApp() {
    AppTheme {
        RealEstateNavigation()
    }
}