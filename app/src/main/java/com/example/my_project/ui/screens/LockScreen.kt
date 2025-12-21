package com.example.my_project.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(
    email: String?,
    onUnlockByBiometricSuccess: () -> Unit,
    onUnlockByPassword: (String, (String?) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    var showPassword by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun startBiometric() {
        val act = activity ?: return
        val executor = ContextCompat.getMainExecutor(act)

        val prompt = BiometricPrompt(
            act,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockByBiometricSuccess()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Разблокировать")
            .setSubtitle("Подтвердите биометрию")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("Отмена")
            .build()

        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Приложение заблокировано", style = MaterialTheme.typography.titleLarge)

        if (!email.isNullOrBlank()) {
            Text("Аккаунт: $email", style = MaterialTheme.typography.bodyMedium)
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        if (!showPassword) {
            Button(
                onClick = {
                    error = null
                    if (biometricAvailable) startBiometric() else showPassword = true
                },
                modifier = Modifier.fillMaxWidth(0.78f)
            ) {
                Text(if (biometricAvailable) "Разблокировать" else "Ввести пароль")
            }

            if (biometricAvailable) {
                OutlinedButton(
                    onClick = { error = null; showPassword = true },
                    modifier = Modifier.fillMaxWidth(0.78f)
                ) { Text("Ввести пароль") }
            }
        }

        AnimatedVisibility(visible = showPassword) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it; error = null },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onUnlockByPassword(pass) { msg -> error = msg } },
                    modifier = Modifier.fillMaxWidth(0.78f)
                ) { Text("Подтвердить") }

                TextButton(onClick = { pass = ""; error = null; showPassword = false }) {
                    Text("Назад")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onLogout) { Text("Сменить аккаунт") }
    }
}