package com.retameur.dartsconnect

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

internal const val dataStoreFileName = "dartsconnect.preferences_pb"

expect fun dataStorePreferences(
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    coroutineScope: CoroutineScope,
): DataStore<Preferences>

internal fun createDataStoreWithDefaults(
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    path: () -> String,
) = PreferenceDataStoreFactory
    .createWithPath(
        corruptionHandler = corruptionHandler,
        scope = coroutineScope,
        produceFile = {
            path().toPath()
        }
    )

val dataStore: DataStore<Preferences> = dataStorePreferences(
    corruptionHandler = null,
    coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
)

val DIRECT_CONNECTION = booleanPreferencesKey("DIRECT_CONNECTION")
val IP = stringPreferencesKey("IP")

object Settings {
    suspend fun setDirectConnection(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[DIRECT_CONNECTION] = value
        }
    }

    suspend fun getDirectConnection() = dataStore.data.map { prefs ->
        prefs[DIRECT_CONNECTION] ?: true
    }.first()
    
    suspend fun setIp(ip: String) {
        dataStore.edit { prefs ->
            prefs[IP] = ip
        }
    }

    suspend fun getIp() = dataStore.data.map { prefs ->
        prefs[IP] ?: ""
    }.first()
}