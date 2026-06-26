@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.real_estate_manager

import androidx.compose.runtime.Composable
import com.example.real_estate_manager.navigation.NotificationNavigationTarget
import com.example.real_estate_manager.navigation.RealEstateNavigation
import com.example.real_estate_manager.ui.theme.AppTheme

@Composable
fun RealEstateApp(
    notificationNavigationTarget: NotificationNavigationTarget? = null,
    onNotificationNavigationHandled: () -> Unit = {}
) {
    AppTheme {
        RealEstateNavigation(
            notificationNavigationTarget = notificationNavigationTarget,
            onNotificationNavigationHandled = onNotificationNavigationHandled
        )
    }
}
