package com.esf.calendar.data.repository

import android.content.Context
import com.esf.calendar.data.local.ESFDatabase
import com.esf.calendar.data.local.PreferencesManager
import com.esf.calendar.data.local.SecurePreferencesManager
import com.esf.calendar.data.model.*
import com.esf.calendar.data.remote.ESFApiService
import com.esf.calendar.data.remote.ESFAuthService
import com.esf.calendar.data.remote.RetrofitClient
import com.esf.calendar.util.Constants
import com.esf.calendar.util.DateParser
import com.esf.calendar.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Repository central pour gérer les données ESF
 * Orchestre les sources de données locales (Room) et distantes (API)
 */
class ESFRepository(private val context: Context) {

    private val database = ESFDatabase.getDatabase(context)
    private val eventDao = database.eventDao()
    private val apiService: ESFApiService = RetrofitClient.apiService
    private val authService = ESFAuthService(context)
    private val securePrefs = SecurePreferencesManager(context)
    private val prefsManager = PreferencesManager(context)

    /**
     * Récupère tous les événements depuis la base locale
     */
    fun getAllEvents(): Flow<List<ESFEvent>> = eventDao.getAllEvents()

    /**
     * Récupère les événements pour une période donnée
     */
    fun getEventsBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<ESFEvent>> =
        eventDao.getEventsBetween(start, end)

    /**
     * Récupère les événements d'aujourd'hui
     */
    fun getTodayEvents(): Flow<List<ESFEvent>> =
        eventDao.getTodayEvents(LocalDateTime.now())

    /**
     * Récupère les prochains événements
     */
    fun getUpcomingEvents(limit: Int = 10): Flow<List<ESFEvent>> =
        eventDao.getUpcomingEvents(LocalDateTime.now(), limit)

    /**
     * Synchronise les événements depuis l'API ESF
     * Télécharge les nouveaux événements et les stocke localement
     */
    suspend fun syncEvents(): Resource<Int> {
        return try {
            // Vérifier les credentials
            val credentials = securePrefs.getCredentials()
                ?: return Resource.Error("Non connecté")

            val moniteurId = securePrefs.getMoniteurId()
                ?: return Resource.Error("ID moniteur non trouvé")

            // Construire la requête
            val now = System.currentTimeMillis()
            val endDate = LocalDateTime.now().plusMonths(Constants.EVENT_FETCH_MONTHS_AHEAD)
            val endMillis = endDate.atZone(ZoneId.of("Europe/Paris")).toInstant().toEpochMilli()

            val methodParams = ESFMethodParams(
                idTecMoniteurList = listOf(moniteurId),
                dateHeureDebut = "/Date($now)/",
                dateHeureFin = "/Date($endMillis)/"
            )

            val request = ESFApiRequest(methodParams = methodParams)

            // Headers avec cookies
            val headers = mutableMapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )

            credentials.cookies?.let { cookies ->
                headers["Cookie"] = cookies
            }

            // Construire l'URL complète
            val fullUrl = "${Constants.ESF_BASE_URL}${Constants.ESF_API_ENDPOINT}" +
                    "?IPlanningParticulierServicePublic&GetListeHorairesMoniteur"

            // Exécuter la requête
            val response = apiService.getHorairesMoniteur(fullUrl, headers, request)

            if (response.isSuccessful && response.body() != null) {
                val esfResponse = response.body()!!

                // Filtrer les absences et parser les dates
                val eventsToInsert = esfResponse.items
                    .filterNot { it.isAbsenceEvent() }
                    .map { event ->
                        event.copy(
                            startDateTime = DateParser.parseESFDate(event.dateDebut),
                            endDateTime = DateParser.parseESFDate(event.dateFin),
                            isAbsence = event.isAbsenceEvent(),
                            syncedAt = System.currentTimeMillis()
                        )
                    }

                // Récupérer les IDs existants pour détecter les nouveaux
                val existingIds = eventDao.getAllEventIds().toSet()
                val newEvents = eventsToInsert.filter { it.horaireId !in existingIds }

                // Insérer dans la base locale
                eventDao.insertEvents(eventsToInsert)

                // Nettoyer les anciens événements (> 30 jours)
                val cutoffDate = LocalDateTime.now().minusDays(30)
                eventDao.deleteOldEvents(cutoffDate)

                // Mettre à jour le timestamp de synchro
                prefsManager.setLastSyncTimestamp(System.currentTimeMillis())

                Resource.Success(newEvents.size)
            } else {
                Resource.Error("Erreur API: ${response.code()}")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error("Erreur de synchronisation: ${e.message}", e)
        }
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isLoggedIn(): Boolean = securePrefs.isLoggedIn()

    /**
     * Récupère les credentials
     */
    fun getCredentials(): ESFCredentials? = securePrefs.getCredentials()

    /**
     * Sauvegarde les credentials
     */
    fun saveCredentials(credentials: ESFCredentials) {
        securePrefs.saveCredentials(credentials)
    }

    /**
     * Sauvegarde les cookies de session
     */
    fun saveCookies(cookies: String) {
        securePrefs.saveCookies(cookies)
    }

    /**
     * Sauvegarde l'ID du moniteur
     */
    fun saveMoniteurId(moniteurId: String) {
        securePrefs.saveMoniteurId(moniteurId)
    }

    /**
     * Déconnexion
     */
    suspend fun logout() {
        securePrefs.logout()
        prefsManager.setLoggedIn(false)
        prefsManager.setMoniteurId(null)
        eventDao.deleteAllEvents()
        authService.clearCookies()
    }

    /**
     * Compte le nombre d'événements
     */
    suspend fun getEventCount(): Int = eventDao.getEventCount()
}
