package com.devqube.drinktracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devqube.drinktracker.ui.theme.LocalDrinkColors
import com.devqube.drinktracker.update.UpdateLogic
import com.devqube.drinktracker.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Aktualizacje z poziomu aplikacji: sprawdza ostatnie wydanie na GitHubie (sama – raz na parę
 * godzin – i na żądanie), pobiera APK, weryfikuje sumę, pakiet, wersję i podpis, po czym oddaje
 * go systemowemu instalatorowi.
 */
@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalDrinkColors.current

    var release by remember { mutableStateOf<UpdateLogic.Release?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }
    val hint = remember { UpdateManager.hint(context) }

    fun check(silent: Boolean) {
        scope.launch {
            busy = true
            if (!silent) status = "Sprawdzam…"
            runCatching { withContext(Dispatchers.IO) { UpdateManager.checkNow(context) } }
                .onSuccess {
                    release = it
                    status = when {
                        it != null -> "Dostępna wersja ${it.version}."
                        silent -> null
                        else -> "Masz najnowszą wersję."
                    }
                }
                .onFailure { if (!silent) status = "Nie udało się sprawdzić – brak internetu albo GitHub nie odpowiada." }
            busy = false
        }
    }

    fun install(r: UpdateLogic.Release) {
        if (!UpdateManager.canInstall(context)) {
            status = "Zezwól Drink Trackerowi na instalowanie aplikacji i naciśnij jeszcze raz."
            UpdateManager.openInstallPermission(context)
            return
        }
        scope.launch {
            busy = true
            progress = 0f
            status = "Pobieram ${r.apkName}…"
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    val apk = UpdateManager.download(context, r) { received, total ->
                        progress = if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else null
                    }
                    UpdateManager.verify(context, apk, r.sha256) to apk
                }
            }
            outcome.onSuccess { (problem, apk) ->
                if (problem == null) {
                    status = "Gotowe – potwierdź instalację w oknie systemu."
                    UpdateManager.install(context, apk)
                } else {
                    status = problem
                }
            }.onFailure { status = "Pobieranie nie powiodło się: ${it.message ?: ""}" }
            progress = null
            busy = false
        }
    }

    // Ciche sprawdzenie przy wejściu w Ustawienia, jeśli ostatnie było dawno.
    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - UpdateManager.lastCheckedMs(context) > 3L * 60 * 60 * 1000) check(silent = true)
    }

    Card {
        Text("Aktualizacje", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Zainstalowana wersja: ${UpdateManager.installedVersionName(context)}" +
                if (hint.isNotEmpty() && release == null) " · czeka wersja $hint" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )

        release?.let { r ->
            Spacer(Modifier.height(10.dp))
            Text(r.title, style = MaterialTheme.typography.titleMedium)
            if (r.notes.isNotBlank()) {
                val notes = UpdateLogic.plainNotes(r.notes)
                Text(
                    notes.take(500) + if (notes.length > 500) "…" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        progress?.let {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { it },
                color = MaterialTheme.colorScheme.primary,
                trackColor = colors.surface2,
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            release?.let { r ->
                Button(
                    onClick = { install(r) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text("Pobierz i zainstaluj", fontWeight = FontWeight.Bold) }
            }
            Button(
                onClick = { check(silent = false) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface2,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            ) { Text("Sprawdź aktualizacje") }
        }
    }
}
