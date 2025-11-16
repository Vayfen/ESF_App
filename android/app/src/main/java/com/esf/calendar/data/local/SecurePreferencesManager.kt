package com.esf.calendar.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.esf.calendar.data.model.ESFCredentials
import com.esf.calendar.util.Constants

/**
 * Gestionnaire de stockage sécurisé pour les credentials ESF
 * Utilise EncryptedSharedPreferences avec Android Keystore
 */
class SecurePreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.ENCRYPTED_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Sauvegarde les credentials ESF de manière sécurisée
     */
    fun saveCredentials(credentials: ESFCredentials) {
        encryptedPrefs.edit().apply {
            putString(KEY_USERNAME, credentials.username)
            putString(KEY_PASSWORD, credentials.password)
            credentials.cookies?.let { putString(KEY_COOKIES, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Récupère les credentials ESF
     */
    fun getCredentials(): ESFCredentials? {
        if (!isLoggedIn()) return null

        val username = encryptedPrefs.getString(KEY_USERNAME, null) ?: return null
        val password = encryptedPrefs.getString(KEY_PASSWORD, null) ?: return null
        val cookies = encryptedPrefs.getString(KEY_COOKIES, null)

        return ESFCredentials(username, password, cookies)
    }

    /**
     * Sauvegarde les cookies de session
     */
    fun saveCookies(cookies: String) {
        encryptedPrefs.edit().putString(KEY_COOKIES, cookies).apply()
    }

    /**
     * Récupère les cookies de session
     */
    fun getCookies(): String? {
        return encryptedPrefs.getString(KEY_COOKIES, null)
    }

    /**
     * Sauvegarde l'ID du moniteur
     */
    fun saveMoniteurId(moniteurId: String) {
        encryptedPrefs.edit().putString(KEY_MONITEUR_ID, moniteurId).apply()
    }

    /**
     * Récupère l'ID du moniteur
     */
    fun getMoniteurId(): String? {
        return encryptedPrefs.getString(KEY_MONITEUR_ID, null)
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isLoggedIn(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Déconnexion : supprime tous les credentials
     */
    fun logout() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_USERNAME = "esf_username"
        private const val KEY_PASSWORD = "esf_password"
        private const val KEY_COOKIES = "esf_cookies"
        private const val KEY_MONITEUR_ID = "moniteur_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
