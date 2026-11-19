package com.devqube.drinktracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devqube.drinktracker.ui.AppRoot
import com.devqube.drinktracker.ui.AppViewModel
import com.devqube.drinktracker.ui.theme.DrinkTrackerTheme

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* wynik nieistotny */ }

    /** Licznik próśb o otwarcie ekranu logowania (np. z przycisku na widgecie). */
    private var openLogRequests by mutableIntStateOf(0)

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

        if (savedInstanceState == null && wantsLog(intent)) openLogRequests++

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            DrinkTrackerTheme(theme = settings.theme, vulgarity = settings.vulgarity) {
                AppRoot(vm, openLogRequests)
            }
        }
    }

    // Aplikacja już działa (singleTop) – przycisk z widgetu ma przełączyć na ekran logowania.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (wantsLog(intent)) openLogRequests++
    }

    companion object {
        /** Osobny URI, żeby PendingIntent przycisku nie zlał się z kliknięciem w resztę widgetu. */
        private val LOG_URI: Uri = Uri.parse("drinktracker://log")

        private fun wantsLog(intent: Intent?) = intent?.data == LOG_URI

        fun logIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .setData(LOG_URI)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
