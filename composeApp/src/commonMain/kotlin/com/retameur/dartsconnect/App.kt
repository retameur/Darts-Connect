package com.retameur.dartsconnect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Wifi1Bar
import androidx.compose.material.icons.rounded.Wifi2Bar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable


// Navigation


@Serializable
object ChooseGameRoute
@Serializable
data class GameRoute(val gameType: String, val nbPlayers: Int) // gameType should be replaced by an enum, but there is some issues on iOS
@Serializable
object SettingsRoute


// App

@Composable
@Preview
fun App(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isConnected by DartboardDartsConnect.isConnected.collectAsStateWithLifecycle()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if(currentDestination?.hasRoute<SettingsRoute>() == true) {
                            Text("Settings")
                        }
                    },
                    navigationIcon = {
                        if (navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ConnectionButton(onClick = {}, isConnected)

                            if(currentDestination?.hasRoute<SettingsRoute>() == false) {
                                IconButton(onClick = {
                                    navController.navigate(SettingsRoute)
                                }) {
                                    Icon(Icons.Rounded.Settings, null)
                                }
                            }
                        }

                    },
                    backgroundColor = Color.Transparent,
                    elevation = 0.dp
                )
            },
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = ChooseGameRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ){
                composable<ChooseGameRoute> {
                    ChooseGameScreen(onSelectGame = { gameType, nbPlayers ->
                        navController.navigate(GameRoute(gameType, nbPlayers))
                    })
                }
                composable<GameRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<GameRoute>()
                    val game = remember { CricketGame(nbPlayers = route.nbPlayers) }
                    when (route.gameType) {
                        "CRICKET" -> CricketGameScreen(game)
                    }
                }
                composable<SettingsRoute> {
                    NetworkConfigScreen()
                }
            }
        }
    }
}

@Composable
fun ConnectionButton(onClick: () -> Unit, isConnected: Boolean) {

    var imageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(!isConnected) {
        while (true) {
            delay(300)
            imageIndex = (imageIndex + 1) % 3
        }
    }

    IconButton(onClick) {
        when {
            isConnected -> Icon(Icons.Rounded.Wifi, null, tint = Color(0f, 0.7f, 0f))
            imageIndex == 0 -> Icon(Icons.Rounded.Wifi1Bar, null)
            imageIndex == 1 -> Icon(Icons.Rounded.Wifi2Bar, null)
            imageIndex == 2 -> Icon(Icons.Rounded.Wifi, null)
        }
    }
}