package com.esf.calendar.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.esf.calendar.ESFCalendarApplication
import com.esf.calendar.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Receiver pour relancer la synchronisation après redémarrage du téléphone
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            // Relancer la synchronisation périodique
            CoroutineScope(Dispatchers.IO).launch {
                val prefsManager = PreferencesManager(context)
                val prefs = prefsManager.userPreferencesFlow.first()

                // Ne relancer que si l'utilisateur est connecté
                if (prefs.isLoggedIn) {
                    scheduleSyncWork(context, prefs.syncFrequency.minutes)
                }
            }
        }
    }

    private fun scheduleSyncWork(context: Context, intervalMinutes: Long) {
        if (intervalMinutes == 0L) return // Mode manuel

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ESFCalendarApplication.SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
