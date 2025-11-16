package com.esf.calendar.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esf.calendar.data.model.ESFEvent
import com.esf.calendar.ui.components.AnimatedGradientBackground
import com.esf.calendar.ui.components.GlassCard
import com.esf.calendar.ui.theme.*
import com.esf.calendar.util.DateParser
import com.esf.calendar.util.Resource
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Écran principal du calendrier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val selectedDayEvents by viewModel.selectedDayEvents.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond animé
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                CalendarTopBar(
                    selectedDate = selectedDate,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onToday = { viewModel.goToToday() },
                    onSync = { viewModel.syncEvents() },
                    onSettings = onNavigateToSettings,
                    syncState = syncState
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Calendrier mensuel
                MonthCalendar(
                    selectedDate = selectedDate,
                    allEvents = allEvents,
                    onDateSelected = { viewModel.selectDate(it) },
                    hasEventsOnDate = { viewModel.hasEventsOnDate(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Liste des événements du jour
                EventsList(
                    events = selectedDayEvents,
                    selectedDate = selectedDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

/**
 * Top bar du calendrier
 */
@Composable
fun CalendarTopBar(
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    syncState: Resource<Int>
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glowEffect = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton mois précédent
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Mois précédent",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Titre du mois
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToday() }
            ) {
                Text(
                    text = selectedDate.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedDate.year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Bouton mois suivant
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Mois suivant",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Bouton aujourd'hui
            OutlinedButton(
                onClick = onToday,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aujourd'hui")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton synchroniser
            Button(
                onClick = onSync,
                enabled = syncState !is Resource.Loading,
                modifier = Modifier.weight(1f)
            ) {
                if (syncState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton paramètres
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Message de statut synchro
        when (syncState) {
            is Resource.Success -> {
                val count = syncState.data
                if (count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$count nouvel${if (count > 1) "les" else ""} événement${if (count > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            is Resource.Error -> {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            else -> {}
        }
    }
}

/**
 * Calendrier mensuel
 */
@Composable
fun MonthCalendar(
    selectedDate: LocalDate,
    allEvents: List<ESFEvent>,
    onDateSelected: (LocalDate) -> Unit,
    hasEventsOnDate: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier
) {
    val yearMonth = YearMonth.of(selectedDate.year, selectedDate.month)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Lundi

    GlassCard(modifier = modifier) {
        Column {
            // En-têtes des jours de la semaine
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grille des jours
            val weeks = (daysInMonth + firstDayOfWeek + 6) / 7
            for (week in 0 until weeks) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = week * 7 + dayOfWeek - firstDayOfWeek
                        if (dayIndex in 0 until daysInMonth) {
                            val date = yearMonth.atDay(dayIndex + 1)
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                hasEvents = hasEventsOnDate(date),
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (week < weeks - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Cellule d'un jour dans le calendrier
 */
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> Brush.linearGradient(
                        colors = listOf(ElectricBlue, NeonPurple)
                    )
                    isToday -> Brush.linearGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.3f),
                            NeonPurple.copy(alpha = 0.3f)
                        )
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> androidx.compose.ui.graphics.Color.White
                    isToday -> ElectricBlue
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            // Indicateur d'événements
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) androidx.compose.ui.graphics.Color.White
                            else ElectricGreen
                        )
                )
            }
        }
    }
}

/**
 * Liste des événements du jour sélectionné
 */
@Composable
fun EventsList(
    events: List<ESFEvent>,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Titre
        Text(
            text = if (selectedDate == LocalDate.now()) "Aujourd'hui"
            else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                .replaceFirstChar { it.uppercase() } + " ${selectedDate.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        if (events.isEmpty()) {
            // Aucun événement
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun événement",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Liste des événements
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.horaireId }) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

/**
 * Carte d'un événement
 */
@Composable
fun EventCard(event: ESFEvent) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        glowEffect = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Barre de couleur à gauche
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(EventBlue, EventPurple)
                        )
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Contenu
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Titre
                Text(
                    text = event.getTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Heure
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    event.startDateTime?.let { start ->
                        event.endDateTime?.let { end ->
                            Text(
                                text = "${DateParser.formatTimeOnly(start)} - ${DateParser.formatTimeOnly(end)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Niveau et langue
                if (event.labelNiveau != null || event.labelLangue != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ElectricGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                event.labelNiveau?.let { append(it) }
                                if (event.labelNiveau != null && event.labelLangue != null) append(" • ")
                                event.labelLangue?.let { append(it) }
                                event.nombreEleves?.let { append(" ($it)") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Lieu
                event.labelLieu?.let { lieu ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonYellow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lieu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
