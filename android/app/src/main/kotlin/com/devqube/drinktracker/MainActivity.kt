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

    /** Licznik próśb o otwarcie karty aktualizacji (z powiadomienia o nowej wersji). */
    private var openUpdatesRequests by mutableIntStateOf(0)

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

        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            DrinkTrackerTheme(theme = settings.theme, vulgarity = settings.vulgarity) {
                AppRoot(vm, openLogRequests, openUpdatesRequests)
            }
        }
    }

    // Aplikacja już działa (singleTop) – przycisk z widgetu ma przełączyć na ekran logowania.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        when (intent?.data) {
            LOG_URI -> openLogRequests++
            UPDATES_URI -> openUpdatesRequests++
        }
    }

    companion object {
        /** Osobny URI, żeby PendingIntent przycisku nie zlał się z kliknięciem w resztę widgetu. */
        private val LOG_URI: Uri = Uri.parse("drinktracker://log")
        private val UPDATES_URI: Uri = Uri.parse("drinktracker://updates")

        fun logIntent(context: Context): Intent = deepLink(context, LOG_URI)

        fun updatesIntent(context: Context): Intent = deepLink(context, UPDATES_URI)

        private fun deepLink(context: Context, uri: Uri): Intent =
            Intent(context, MainActivity::class.java)
                .setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
