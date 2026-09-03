package dev.amenokizele.tervyn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import dev.amenokizele.tervyn.demo.DemoRepository
import dev.amenokizele.tervyn.navigation.TervynNavGraph
import dev.amenokizele.tervyn.ui.theme.TervynTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by DemoRepository.themeMode.collectAsState()
            TervynTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                TervynNavGraph(navController = navController)
            }
        }
    }
}
