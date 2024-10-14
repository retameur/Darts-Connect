package com.retameur.dartsconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch


@Composable
fun NetworkConfigScreen() {

    val scope = rememberCoroutineScope()
    val ip = DartboardDartsConnect.ip.collectAsStateWithLifecycle()
    var ipTmp by remember { mutableStateOf(ip.value) }

    val directConnection by DartboardDartsConnect.directConnection.collectAsStateWithLifecycle()
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val isConnected = DartboardDartsConnect.isConnected

    LaunchedEffect(ip.value) {
        ipTmp = ip.value
    }

    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.selectable(
                    selected = directConnection,
                    onClick = { DartboardDartsConnect.setDirectConnection() },
                    role = Role.RadioButton
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(directConnection, onClick = null)
                Text("Target AP (direct)")
            }

            Row(
                Modifier.selectable(
                    selected = !directConnection,
                    onClick = { DartboardDartsConnect.setLocalNetworkConnection("") },
                    role = Role.RadioButton
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(!directConnection, onClick = null)
                Text("Local Network")
            }
        }
        TextField(
            ipTmp,
            onValueChange = { ipTmp = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !directConnection,
            label = { Text("Target IP") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                DartboardDartsConnect.setLocalNetworkConnection(ipTmp)
            }),
            singleLine = true
        )

        Divider()

        TextField(
            ssid,
            onValueChange = { ssid = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SSID") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            singleLine = true
        )
        TextField(
            password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            singleLine = true
        )
        Button(
            onClick = {
                scope.launch {
                    DartboardDartsConnect.configWifi(ssid, password)
                }
            },
            enabled = isConnected.value && ssid.isNotBlank() && password.isNotBlank(),
        ) {
            Text("Configure Target's WiFi")
        }
    }
}
