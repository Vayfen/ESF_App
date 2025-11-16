package com.esf.calendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.esf.calendar.worker.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Classe Application principale de l'app ESF Calendar
 * Initialise les composants globaux au démarrage
 */
class ESFCalendarApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Créer les canaux de notification
        createNotificationChannels()

        // Démarrer la synchronisation périodique
        scheduleSyncWork()
    }

    /**
     * Configuration WorkManager
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Créer les canaux de notification (Android 8.0+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Canal pour les notifications de synchronisation
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "Synchronisation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications de synchronisation des événements ESF"
                setShowBadge(false)
            }

            // Canal pour les nouveaux événements
            val eventsChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "Nouveaux événements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications lors de l'ajout de nouveaux événements"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(syncChannel)
            notificationManager.createNotificationChannel(eventsChannel)
        }
    }

    /**
     * Planifier la synchronisation périodique avec WorkManager
     */
    private fun scheduleSyncWork() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, // Fréquence par défaut : 15 minutes
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    companion object {
        const val CHANNEL_ID_SYNC = "sync_channel"
        const val CHANNEL_ID_EVENTS = "events_channel"
        const val SYNC_WORK_NAME = "esf_calendar_sync"
    }
}
