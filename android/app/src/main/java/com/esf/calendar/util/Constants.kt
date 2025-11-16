package com.esf.calendar.util

/**
 * Constantes globales de l'application
 */
object Constants {
    // URLs
    // IMPORTANT: Utiliser esf356.w-esf.com (pas carnet-rouge-esf.app)
    // pour compatibilité avec l'API de planning comme dans le script Python
    const val ESF_BASE_URL = "https://esf356.w-esf.com/"
    const val ESF_LOGIN_URL = "https://esf356.w-esf.com/PlanningParticulierSSO/PlanningParticulier.aspx"
    const val ESF_API_ENDPOINT = "AjaxProxyService.svc/InvokeMethod"

    // API Parameters (valeurs par défaut de ton script Python)
    const val ID_GEN_POSTE_TECHNIQUE = "6862462"
    const val ID_COM_LANGUE = "1"
    const val ID_COM_SAISON = "63"
    const val NO_ECOLE = "356"
    const val CODE_UC = "TECH-UC002-M"
    const val CODE_APPLICATION = "PLANNING-PARTICULIER-MONITEUR"

    // Database
    const val DATABASE_NAME = "esf_calendar_db"
    const val DATABASE_VERSION = 1

    // Preferences
    const val PREFS_NAME = "esf_calendar_prefs"
    const val ENCRYPTED_PREFS_NAME = "encrypted_prefs"

    // Notification IDs
    const val NOTIFICATION_ID_SYNC = 1001
    const val NOTIFICATION_ID_NEW_EVENT = 1002

    // WorkManager
    const val SYNC_WORKER_TAG = "sync_worker"
    const val SYNC_WORK_NAME = "esf_calendar_sync"

    // Date range (récupère 4 mois d'événements)
    const val EVENT_FETCH_MONTHS_AHEAD = 4L
}
