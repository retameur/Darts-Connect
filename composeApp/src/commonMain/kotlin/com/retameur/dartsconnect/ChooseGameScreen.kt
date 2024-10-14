package com.retameur.dartsconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun ChooseGameScreen(onSelectGame: (gameType: String, nbPlayers: Int) -> Unit) {

    var nbPlayers by remember { mutableStateOf(2) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Number of players :")
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { nbPlayers -= 1 },
                    enabled = nbPlayers >= 2
                ) {
                    Icon(Icons.Filled.RemoveCircle, contentDescription = "Add")
                }

                Text("$nbPlayers")

                IconButton(
                    onClick = { nbPlayers += 1 },
                    enabled = nbPlayers < 4
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Add")
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Select a game :")
            Button(onClick = {
                onSelectGame("CRICKET", nbPlayers)
            }) {
                Text("Cricket")
            }
        }
    }

}