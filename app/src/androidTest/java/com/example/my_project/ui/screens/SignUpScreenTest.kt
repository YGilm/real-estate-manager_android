package com.example.my_project.ui.screens

import com.example.my_project.ui.TestComposeActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun displaysKeyElements() {
        composeRule.setContent {
            SignUpScreen(
                onSignUp = { _, _, _, onDone -> onDone(null) },
                onBack = {}
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Регистрация").assertIsDisplayed()
        composeRule.onNodeWithTag("SignUpEmail").assertIsDisplayed()
        composeRule.onNodeWithTag("SignUpPassword").assertIsDisplayed()
        composeRule.onNodeWithTag("SignUpPasswordConfirm").assertIsDisplayed()
        composeRule.onNodeWithText("Запомнить меня").assertIsDisplayed()
        composeRule.onNodeWithText("Назад").assertIsDisplayed()
        composeRule.onNodeWithText("Создать").assertIsDisplayed()
    }

    @Test
    fun inputsAndSubmitPassValues() {
        var capturedEmail: String? = null
        var capturedPass: String? = null
        var capturedRemember: Boolean? = null

        composeRule.setContent {
            SignUpScreen(
                onSignUp = { email, pass, remember, onDone ->
                    capturedEmail = email
                    capturedPass = pass
                    capturedRemember = remember
                    onDone(null)
                },
                onBack = {}
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("SignUpEmail").performTextInput("user@example.com")
        composeRule.onNodeWithTag("SignUpPassword").performTextInput("secret123")
        composeRule.onNodeWithTag("SignUpPasswordConfirm").performTextInput("secret123")
        composeRule.onNodeWithTag("SignUpRemember").performClick()
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.runOnIdle {
            assertEquals("user@example.com", capturedEmail)
            assertEquals("secret123", capturedPass)
            assertEquals(false, capturedRemember)
        }
    }

    @Test
    fun showsErrorFromCallback() {
        composeRule.setContent {
            SignUpScreen(
                onSignUp = { _, _, _, onDone -> onDone("Ошибка регистрации") },
                onBack = {}
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Создать").performClick()
        composeRule.onNodeWithText("Ошибка регистрации").assertIsDisplayed()
    }
}
