package com.example.real_estate_manager.ui

import com.example.real_estate_manager.ui.TestComposeActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.real_estate_manager.ui.screens.SignInScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun signInNavigatesToHome() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "signin") {
                composable("signin") {
                    SignInScreen(
                        onSignIn = { _, _, _, onDone ->
                            onDone(null)
                            navController.navigate("home")
                        },
                        onGoSignUp = {}
                    )
                }
                composable("home") { HomeStub() }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction().and(hasText("Email"))).performTextInput("user@example.com")
        composeRule.onNode(hasSetTextAction().and(hasText("Пароль"))).performTextInput("pass123")
        composeRule.onNodeWithText("Войти").performClick()
        composeRule.onNodeWithText("Моя недвижимость").assertIsDisplayed()
    }
}

@Composable
private fun HomeStub() {
    Text("Моя недвижимость")
}
