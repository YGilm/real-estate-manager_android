package com.example.real_estate_manager.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
        val act = activity
        if (act == null) {
            error = "Биометрия недоступна"
            showPassword = true
            return
        }
        val executor = ContextCompat.getMainExecutor(act)

        val prompt = BiometricPrompt(
            act,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlockByBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    error = "Биометрия не распознана"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
            }
        }

        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) { Text("Сменить аккаунт") }
    }
}
