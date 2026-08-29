package com.dhan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import com.dhan.app.data.prefs.LocalUserPrefs
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.nav.DhanNavHost
import com.dhan.app.nav.Routes
import com.dhan.app.ui.theme.DhanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DhanApplication
        val startDestination = if (app.userPrefs.hasOnboarded) Routes.LOGIN else Routes.SPLASH

        setContent {
            CompositionLocalProvider(
                LocalRepository provides app.repository,
                LocalUserPrefs provides app.userPrefs,
            ) {
                DhanApp(startDestination = startDestination)
            }
        }
    }
}

@Composable
private fun DhanApp(startDestination: String) {
    DhanTheme {
        DhanNavHost(startDestination = startDestination)
    }
}
