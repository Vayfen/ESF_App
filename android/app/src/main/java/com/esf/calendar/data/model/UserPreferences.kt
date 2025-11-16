package com.esf.calendar.data.model

/**
 * Modèle pour les préférences utilisateur
 */
data class UserPreferences(
    val isLoggedIn: Boolean = false,
    val moniteurId: String? = null,

    // Paramètres de synchronisation
    val syncFrequency: SyncFrequency = SyncFrequency.FIFTEEN_MINUTES,
    val syncStartHour: Int = 7, // 7h du matin
    val syncEndHour: Int = 20, // 20h du soir
    val wifiOnly: Boolean = false,
    val batteryOptimization: Boolean = true, // Ne pas synchro si batterie < 15%

    // Affichage
    val showGoogleCalendar: Boolean = true,

    // Notifications
    val notificationsEnabled: Boolean = true,

    // Dernière synchro
    val lastSyncTimestamp: Long = 0L
)

/**
 * Fréquence de synchronisation
 */
enum class SyncFrequency(val minutes: Long) {
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    MANUAL(0) // Pas de synchro automatique
}

/**
 * Credentials ESF stockés de manière sécurisée
 */
data class ESFCredentials(
    val username: String,
    val password: String,
    val cookies: String? = null // Cookies de session après login
)
