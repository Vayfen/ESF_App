package com.esf.calendar.data.local

import androidx.room.*
import com.esf.calendar.data.model.ESFEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO pour les événements ESF
 * Gère l'accès à la base de données locale
 */
@Dao
interface ESFEventDao {

    /**
     * Récupère tous les événements (sans les absences)
     * Retourne un Flow pour observer les changements en temps réel
     */
    @Query("""
        SELECT * FROM esf_events
        WHERE isAbsence = 0
        ORDER BY startDateTime ASC
    """)
    fun getAllEvents(): Flow<List<ESFEvent>>

    /**
     * Récupère les événements pour une période donnée
     */
    @Query("""
        SELECT * FROM esf_events
        WHERE isAbsence = 0
        AND startDateTime >= :startDate
        AND startDateTime <= :endDate
        ORDER BY startDateTime ASC
    """)
    fun getEventsBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<ESFEvent>>

    /**
     * Récupère un événement par son ID
     */
    @Query("SELECT * FROM esf_events WHERE horaireId = :horaireId")
    suspend fun getEventById(horaireId: Long): ESFEvent?

    /**
     * Récupère tous les IDs d'événements existants
     * Utile pour détecter les nouveaux événements lors de la synchro
     */
    @Query("SELECT horaireId FROM esf_events")
    suspend fun getAllEventIds(): List<Long>

    /**
     * Insère ou met à jour des événements
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ESFEvent>)

    /**
     * Insère ou met à jour un événement
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ESFEvent)

    /**
     * Supprime un événement
     */
    @Delete
    suspend fun deleteEvent(event: ESFEvent)

    /**
     * Supprime tous les événements
     */
    @Query("DELETE FROM esf_events")
    suspend fun deleteAllEvents()

    /**
     * Supprime les anciens événements (plus de 30 jours dans le passé)
     */
    @Query("""
        DELETE FROM esf_events
        WHERE startDateTime < :cutoffDate
    """)
    suspend fun deleteOldEvents(cutoffDate: LocalDateTime)

    /**
     * Compte le nombre d'événements
     */
    @Query("SELECT COUNT(*) FROM esf_events WHERE isAbsence = 0")
    suspend fun getEventCount(): Int

    /**
     * Récupère les événements d'aujourd'hui
     */
    @Query("""
        SELECT * FROM esf_events
        WHERE isAbsence = 0
        AND date(startDateTime) = date(:today)
        ORDER BY startDateTime ASC
    """)
    fun getTodayEvents(today: LocalDateTime): Flow<List<ESFEvent>>

    /**
     * Récupère les prochains événements (à venir)
     */
    @Query("""
        SELECT * FROM esf_events
        WHERE isAbsence = 0
        AND startDateTime >= :now
        ORDER BY startDateTime ASC
        LIMIT :limit
    """)
    fun getUpcomingEvents(now: LocalDateTime, limit: Int = 10): Flow<List<ESFEvent>>
}
