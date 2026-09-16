package com.pulse.budget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "pulse_prefs")

object PrefsKeys {
    // Device-level settings — not tied to a particular account, so these stay as single keys.
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val SESSION_USER_ID = longPreferencesKey("session_user_id")

    // Everything below is per-account: the key name itself is suffixed with the userId, so
    // switching accounts on the same device can never read or overwrite another account's
    // budget, streak or XP. This was the other half of the "data from the previous account
    // shows up after logging out and registering a new user" bug — these used to be single
    // global keys shared by every account.
    fun monthlyBudget(userId: Long) = floatPreferencesKey("monthly_budget_$userId")
    fun minMonthlyBudget(userId: Long) = floatPreferencesKey("min_monthly_budget_$userId")
    fun streakDays(userId: Long) = intPreferencesKey("streak_days_$userId")
    fun xp(userId: Long) = intPreferencesKey("xp_$userId")
    fun lastLogEpochDay(userId: Long) = longPreferencesKey("last_log_epoch_day_$userId")
}

class PrefsRepository(private val context: Context) {
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[PrefsKeys.DARK_THEME] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.DARK_THEME] = enabled }
    }

    fun monthlyBudget(userId: Long): Flow<Float> =
        context.dataStore.data.map { it[PrefsKeys.monthlyBudget(userId)] ?: 10000f }

    suspend fun setMonthlyBudget(userId: Long, amount: Float) {
        context.dataStore.edit { it[PrefsKeys.monthlyBudget(userId)] = amount }
    }

    /** Part 2 brief: "a minimum monthly goal for money spent, as well as a maximum goal." */
    fun minMonthlyBudget(userId: Long): Flow<Float> =
        context.dataStore.data.map { it[PrefsKeys.minMonthlyBudget(userId)] ?: 2000f }

    suspend fun setMinMonthlyBudget(userId: Long, amount: Float) {
        context.dataStore.edit { it[PrefsKeys.minMonthlyBudget(userId)] = amount }
    }

    fun streakDays(userId: Long): Flow<Int> =
        context.dataStore.data.map { it[PrefsKeys.streakDays(userId)] ?: 0 }

    fun xp(userId: Long): Flow<Int> =
        context.dataStore.data.map { it[PrefsKeys.xp(userId)] ?: 0 }

    /** Call once per logged expense. Increments streak only on a new calendar day, always adds XP. */
    suspend fun recordLogEvent(userId: Long, todayEpochDay: Long, xpForLog: Int = 15) {
        context.dataStore.edit { prefs ->
            val lastDayKey = PrefsKeys.lastLogEpochDay(userId)
            val streakKey = PrefsKeys.streakDays(userId)
            val xpKey = PrefsKeys.xp(userId)
            val lastDay = prefs[lastDayKey] ?: -1L
            val currentStreak = prefs[streakKey] ?: 0
            prefs[streakKey] = when (todayEpochDay - lastDay) {
                0L -> currentStreak            // already logged today
                1L -> currentStreak + 1        // consecutive day
                else -> 1                      // streak broken, restart
            }
            prefs[lastDayKey] = todayEpochDay
            prefs[xpKey] = (prefs[xpKey] ?: 0) + xpForLog
        }
    }

    /** Session user id, null when logged out. Drives which screen MainActivity starts on. */
    val sessionUserId: Flow<Long?> = context.dataStore.data.map { it[PrefsKeys.SESSION_USER_ID] }

    suspend fun setSessionUserId(userId: Long?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(PrefsKeys.SESSION_USER_ID)
            else prefs[PrefsKeys.SESSION_USER_ID] = userId
        }
    }
}