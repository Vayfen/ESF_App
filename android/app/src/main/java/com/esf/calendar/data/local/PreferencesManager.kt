package com.esf.calendar.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.esf.calendar.data.model.SyncFrequency
import com.esf.calendar.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Gestionnaire des préférences utilisateur (non sensibles)
 * Utilise DataStore pour un accès réactif et type-safe
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {

    private val dataStore = context.dataStore

    /**
     * Flow des préférences utilisateur
     * S'actualise automatiquement lors des changements
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                isLoggedIn = preferences[PREF_IS_LOGGED_IN] ?: false,
                moniteurId = preferences[PREF_MONITEUR_ID],
                syncFrequency = SyncFrequency.valueOf(
                    preferences[PREF_SYNC_FREQUENCY] ?: SyncFrequency.FIFTEEN_MINUTES.name
                ),
                syncStartHour = preferences[PREF_SYNC_START_HOUR] ?: 7,
                syncEndHour = preferences[PREF_SYNC_END_HOUR] ?: 20,
                wifiOnly = preferences[PREF_WIFI_ONLY] ?: false,
                batteryOptimization = preferences[PREF_BATTERY_OPTIMIZATION] ?: true,
                showGoogleCalendar = preferences[PREF_SHOW_GOOGLE_CALENDAR] ?: true,
                notificationsEnabled = preferences[PREF_NOTIFICATIONS_ENABLED] ?: true,
                lastSyncTimestamp = preferences[PREF_LAST_SYNC] ?: 0L
            )
        }

    /**
     * Met à jour le statut de connexion
     */
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_IS_LOGGED_IN] = isLoggedIn
        }
    }

    /**
     * Met à jour l'ID du moniteur
     */
    suspend fun setMoniteurId(moniteurId: String?) {
        dataStore.edit { preferences ->
            if (moniteurId != null) {
                preferences[PREF_MONITEUR_ID] = moniteurId
            } else {
                preferences.remove(PREF_MONITEUR_ID)
            }
        }
    }

    /**
     * Met à jour la fréquence de synchronisation
     */
    suspend fun setSyncFrequency(frequency: SyncFrequency) {
        dataStore.edit { preferences ->
            preferences[PREF_SYNC_FREQUENCY] = frequency.name
        }
    }

    /**
     * Met à jour les heures de synchronisation
     */
    suspend fun setSyncHours(startHour: Int, endHour: Int) {
        dataStore.edit { preferences ->
            preferences[PREF_SYNC_START_HOUR] = startHour
            preferences[PREF_SYNC_END_HOUR] = endHour
        }
    }

    /**
     * Active/désactive le mode WiFi uniquement
     */
    suspend fun setWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_WIFI_ONLY] = enabled
        }
    }

    /**
     * Active/désactive l'optimisation batterie
     */
    suspend fun setBatteryOptimization(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_BATTERY_OPTIMIZATION] = enabled
        }
    }

    /**
     * Active/désactive l'affichage de Google Calendar
     */
    suspend fun setShowGoogleCalendar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_SHOW_GOOGLE_CALENDAR] = enabled
        }
    }

    /**
     * Active/désactive les notifications
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREF_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Met à jour le timestamp de la dernière synchronisation
     */
    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PREF_LAST_SYNC] = timestamp
        }
    }

    /**
     * Réinitialise toutes les préférences
     */
    suspend fun clearPreferences() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val PREF_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val PREF_MONITEUR_ID = stringPreferencesKey("moniteur_id")
        private val PREF_SYNC_FREQUENCY = stringPreferencesKey("sync_frequency")
        private val PREF_SYNC_START_HOUR = intPreferencesKey("sync_start_hour")
        private val PREF_SYNC_END_HOUR = intPreferencesKey("sync_end_hour")
        private val PREF_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val PREF_BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")
        private val PREF_SHOW_GOOGLE_CALENDAR = booleanPreferencesKey("show_google_calendar")
        private val PREF_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val PREF_LAST_SYNC = longPreferencesKey("last_sync")
    }
}
