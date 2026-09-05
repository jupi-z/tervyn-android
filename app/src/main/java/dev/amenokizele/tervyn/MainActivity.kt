package dev.amenokizele.tervyn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.amenokizele.tervyn.app.TervynApp
import dev.amenokizele.tervyn.app.TervynAppViewModel
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: TervynAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState by appViewModel.state.collectAsStateWithLifecycle()
            TervynTheme(themeMode = appState.themeMode) {
                TervynApp(
                    state = appState,
                    onRetryLocalData = appViewModel::retryLocalDataInitialization,
                    onLogoutConfirmed = appViewModel::logout
                )
            }
        }
    }
}
