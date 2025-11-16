package com.esf.calendar.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esf.calendar.data.model.ESFEvent
import com.esf.calendar.data.repository.ESFRepository
import com.esf.calendar.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ViewModel pour l'écran du calendrier
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ESFRepository(application)

    // État de synchronisation
    private val _syncState = MutableStateFlow<Resource<Int>>(Resource.Loading)
    val syncState: StateFlow<Resource<Int>> = _syncState.asStateFlow()

    // Événements du mois sélectionné
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Tous les événements
    val allEvents: StateFlow<List<ESFEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Événements du jour sélectionné
    val selectedDayEvents: StateFlow<List<ESFEvent>> = combine(
        allEvents,
        selectedDate
    ) { events, date ->
        events.filter { event ->
            event.startDateTime?.toLocalDate() == date
        }.sortedBy { it.startDateTime }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Événements à venir (prochains 7 jours)
    val upcomingEvents: StateFlow<List<ESFEvent>> = repository.getUpcomingEvents(20)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Synchroniser les événements au démarrage
        syncEvents()
    }

    /**
     * Synchronise les événements depuis l'API
     */
    fun syncEvents() {
        android.util.Log.d("CalendarViewModel", "=== syncEvents() appelé ===")
        viewModelScope.launch {
            android.util.Log.d("CalendarViewModel", "Mise à jour état : Loading")
            _syncState.value = Resource.Loading

            android.util.Log.d("CalendarViewModel", "Appel repository.syncEvents()")
            val result = repository.syncEvents()
            android.util.Log.d("CalendarViewModel", "Résultat reçu: $result")
            _syncState.value = result
        }
    }

    /**
     * Change la date sélectionnée
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Navigue au mois précédent
     */
    fun previousMonth() {
        _selectedDate.value = _selectedDate.value.minusMonths(1)
    }

    /**
     * Navigue au mois suivant
     */
    fun nextMonth() {
        _selectedDate.value = _selectedDate.value.plusMonths(1)
    }

    /**
     * Retourne à aujourd'hui
     */
    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    /**
     * Vérifie si une date a des événements
     */
    fun hasEventsOnDate(date: LocalDate): Boolean {
        return allEvents.value.any { event ->
            event.startDateTime?.toLocalDate() == date
        }
    }

    /**
     * Compte les événements pour une date
     */
    fun getEventCountForDate(date: LocalDate): Int {
        return allEvents.value.count { event ->
            event.startDateTime?.toLocalDate() == date
        }
    }
}
