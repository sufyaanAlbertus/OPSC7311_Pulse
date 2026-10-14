package com.pulse.budget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "pulse_prefs")

object PrefsKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
}

class PrefsRepository(private val context: Context) {
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[PrefsKeys.DARK_THEME] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.DARK_THEME] = enabled }
    }
}
