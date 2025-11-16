package com.esf.calendar.worker

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.esf.calendar.ESFCalendarApplication
import com.esf.calendar.R
import com.esf.calendar.data.local.PreferencesManager
import com.esf.calendar.data.repository.ESFRepository
import com.esf.calendar.util.Resource
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Worker pour la synchronisation périodique des événements ESF
 * Gère les contraintes de batterie, réseau et plages horaires
 */
class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = ESFRepository(context)
    private val prefsManager = PreferencesManager(context)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        try {
            // Charger les préférences
            val prefs = prefsManager.userPreferencesFlow.first()

            // Vérifier si on est dans les heures de synchro
            val currentHour = LocalTime.now().hour
            if (currentHour < prefs.syncStartHour || currentHour > prefs.syncEndHour) {
                return Result.success()
            }

            // Vérifier les contraintes de réseau
            if (prefs.wifiOnly && !isWifiConnected()) {
                return Result.retry()
            }

            // Vérifier le niveau de batterie
            if (prefs.batteryOptimization && getBatteryLevel() < 15) {
                return Result.retry()
            }

            // Afficher la notification de synchro en cours
            setForeground(createForegroundInfo())

            // Synchroniser les événements
            when (val result = repository.syncEvents()) {
                is Resource.Success -> {
                    val newEventsCount = result.data

                    // Si nouveaux événements et notifications activées
                    if (newEventsCount > 0 && prefs.notificationsEnabled) {
                        showNewEventsNotification(newEventsCount)
                    }

                    notificationManager.cancel(SYNC_NOTIFICATION_ID)
                    return Result.success()
                }
                is Resource.Error -> {
                    // Afficher une notification d'erreur si critique
                    if (result.message.contains("Non connecté")) {
                        // Ne pas montrer d'erreur si juste pas connecté
                        notificationManager.cancel(SYNC_NOTIFICATION_ID)
                    } else {
                        showErrorNotification(result.message)
                    }
                    return Result.retry()
                }
                is Resource.Loading -> {
                    return Result.retry()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showErrorNotification("Erreur: ${e.message}")
            return Result.failure()
        }
    }

    /**
     * Créer la notification de foreground service
     */
    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, ESFCalendarApplication.CHANNEL_ID_SYNC)
            .setContentTitle("Synchronisation ESF")
            .setContentText("Récupération des événements en cours...")
            .setSmallIcon(R.drawable.ic_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
    }

    /**
     * Afficher une notification pour les nouveaux événements
     */
    private fun showNewEventsNotification(count: Int) {
        val notification = NotificationCompat.Builder(context, ESFCalendarApplication.CHANNEL_ID_EVENTS)
            .setContentTitle("Nouveaux événements ESF")
            .setContentText("$count nouveau${if (count > 1) "x" else ""} événement${if (count > 1) "s" else ""}")
            .setSmallIcon(R.drawable.ic_event)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NEW_EVENT_NOTIFICATION_ID, notification)
    }

    /**
     * Afficher une notification d'erreur
     */
    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(context, ESFCalendarApplication.CHANNEL_ID_SYNC)
            .setContentTitle("Erreur de synchronisation")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_error)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    /**
     * Vérifie si le WiFi est connecté
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Récupère le niveau de batterie (0-100)
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    companion object {
        private const val SYNC_NOTIFICATION_ID = 1001
        private const val NEW_EVENT_NOTIFICATION_ID = 1002
        private const val ERROR_NOTIFICATION_ID = 1003
    }
}
