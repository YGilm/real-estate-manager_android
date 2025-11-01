package com.example.my_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.my_project.auth.AuthRepository
import com.example.my_project.auth.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepo: AuthRepository

    @Inject
    lateinit var userSession: UserSession

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) {
            // автологаут при сворачивании, если "Запомнить меня" выключен
            val remember = runBlocking { userSession.rememberFlow.first() }
            if (!remember) {
                runBlocking { authRepo.logout() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        setContent { RealEstateApp() }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onDestroy()
    }
}