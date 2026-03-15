package com.github.origamiwolf.lomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.origamiwolf.lomp.data.DiceComboRepository
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.data.OracleRepository
import com.github.origamiwolf.lomp.ui.dice.DiceScreen
import com.github.origamiwolf.lomp.ui.oracle.OracleScreen
import com.github.origamiwolf.lomp.ui.settings.SettingsScreen
import com.github.origamiwolf.lomp.ui.theme.LoMPTheme

sealed class Screen(val route: String, val label: String) {
    object Dice : Screen("dice", "Dice")
    object Oracle : Screen("oracle", "Oracle")
    object Settings : Screen("settings", "Settings")
}

class MainActivity : ComponentActivity() {

    private lateinit var dicePreferencesRepository: DicePreferencesRepository
    private lateinit var diceComboRepository: DiceComboRepository
    private lateinit var oracleRepository: OracleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dicePreferencesRepository = DicePreferencesRepository(applicationContext)
        diceComboRepository = DiceComboRepository(applicationContext)
        oracleRepository = OracleRepository(applicationContext)

        setContent {
            LoMPTheme {
                LoMPApp(
                    dicePreferencesRepository = dicePreferencesRepository,
                    diceComboRepository = diceComboRepository,
                    oracleRepository = oracleRepository
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoMPApp(
    dicePreferencesRepository: DicePreferencesRepository,
    diceComboRepository: DiceComboRepository,
    oracleRepository: OracleRepository
) {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Dice, Screen.Oracle)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LoMP") },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                navController.navigate(Screen.Settings.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Hide bottom bar on settings screen
            if (currentRoute != Screen.Settings.route) {
                NavigationBar {
                    tabs.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    Screen.Dice -> Icon(
                                        Icons.Default.Casino,
                                        contentDescription = screen.label
                                    )
                                    Screen.Oracle -> Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = screen.label
                                    )
                                    else -> {}
                                }
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(
                                        navController.graph.findStartDestination().id
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dice.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dice.route) {
                DiceScreen(
                    dicePreferencesRepository = dicePreferencesRepository,
                    diceComboRepository = diceComboRepository
                )
            }
            composable(Screen.Oracle.route) {
                OracleScreen(
                    oracleRepository = oracleRepository,
                    dicePreferencesRepository = dicePreferencesRepository
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(diceComboRepository = diceComboRepository)
            }
        }
    }
}