package com.devqube.drinktracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devqube.drinktracker.ui.AppRoot
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.theme.DrinkTrackerTheme

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* wynik nieistotny */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aplikacja jest zawsze ciemna – jasne ikony paska statusu i nawigacji.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            DrinkTrackerTheme(theme = settings.theme) {
                AppRoot(vm)
            }
        }
    }
}
