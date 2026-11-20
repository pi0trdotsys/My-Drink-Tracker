package com.devqube.drinktracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.devqube.drinktracker.ui.screens.HomeScreen
import com.devqube.drinktracker.ui.screens.LogScreen
import com.devqube.drinktracker.ui.screens.SettingsScreen
import com.devqube.drinktracker.ui.theme.LocalDrinkColors

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Tydzień", Icons.Default.Home),
    LOG("Zaloguj", Icons.Default.Add),
    SETTINGS("Ustawienia", Icons.Default.Settings),
}

@Composable
fun AppRoot(vm: AppViewModel, openLogRequests: Int = 0, openUpdatesRequests: Int = 0) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    // Prośba z widgetu („+ Przyznaję się” / „+ Dodaj”) – przejdź na ekran logowania.
    LaunchedEffect(openLogRequests) { if (openLogRequests > 0) tab = Tab.LOG }
    // Powiadomienie o aktualizacji – Ustawienia, przewinięte do karty aktualizacji.
    LaunchedEffect(openUpdatesRequests) { if (openUpdatesRequests > 0) tab = Tab.SETTINGS }
    val colors = LocalDrinkColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = colors.muted,
                            unselectedTextColor = colors.muted,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        val content = Modifier.padding(inner)
        when (tab) {
            Tab.HOME -> HomeScreen(vm, content)
            Tab.LOG -> LogScreen(vm, content)
            Tab.SETTINGS -> SettingsScreen(vm, content, scrollToUpdatesRequest = openUpdatesRequests)
        }
    }
}
