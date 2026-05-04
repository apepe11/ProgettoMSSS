package com.example.progetto.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val LAST_SESSION_ID = stringPreferencesKey("last_session_id")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val lastSessionId: Flow<String?> = context.dataStore.data.map { it[LAST_SESSION_ID] }

    suspend fun saveUser(userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = userId
            prefs[USERNAME] = username
        }
    }

    suspend fun saveLastSessionId(sessionId: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SESSION_ID] = sessionId
        }
    }

    suspend fun clearLastSessionId() {
        context.dataStore.edit { prefs ->
            prefs.remove(LAST_SESSION_ID)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
