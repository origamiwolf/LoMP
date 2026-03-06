package com.github.origamiwolf.lomp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.ui.dice.DiceScreen
import com.github.origamiwolf.lomp.ui.oracle.OracleScreen
import com.github.origamiwolf.lomp.ui.theme.LoMPTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

sealed class Screen(val route: String, val label: String) {
    object Dice : Screen("dice", "Dice")
    object Oracle : Screen("oracle", "Oracle")
}

class MainActivity : ComponentActivity() {

    // Created once for the lifetime of the Activity.
    // applicationContext is used rather than 'this' to avoid
    // accidentally holding a reference to the Activity after
    // it's destroyed — applicationContext lives as long as the app.
    private lateinit var diceRepository: DicePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        diceRepository = DicePreferencesRepository(applicationContext)

        setContent {
            LoMPTheme {
                LoMPApp(diceRepository = diceRepository)
            }
        }
    }
}

@Composable
fun LoMPApp(diceRepository: DicePreferencesRepository) {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Dice, Screen.Oracle)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

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
                            }
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dice.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dice.route) {
                DiceScreen(diceRepository = diceRepository)
            }
            composable(Screen.Oracle.route) {
                OracleScreen()
            }
        }
    }
}