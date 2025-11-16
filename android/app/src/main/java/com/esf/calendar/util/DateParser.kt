package com.esf.calendar.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Utilitaire pour parser les dates au format Microsoft JSON
 * Format: /Date(milliseconds+offset)/
 * Exemple: /Date(1741158000000+0100)/
 */
object DateParser {

    private val PATTERN = Pattern.compile("/Date\\((\\d+)([+-]\\d{4})?\\)/")
    private val PARIS_ZONE = ZoneId.of("Europe/Paris")

    /**
     * Parse une date ESF au format /Date(ms+offset)/ vers LocalDateTime
     *
     * @param esfDate La date au format ESF
     * @return LocalDateTime en timezone Europe/Paris
     */
    fun parseESFDate(esfDate: String): LocalDateTime? {
        return try {
            val matcher = PATTERN.matcher(esfDate)
            if (!matcher.find()) {
                return null
            }

            val timestampMs = matcher.group(1)?.toLongOrNull() ?: return null
            val offsetStr = matcher.group(2) ?: "+0000"

            // Parser l'offset (+0100 -> +01:00)
            val offsetHours = offsetStr.substring(1, 3).toInt()
            val offsetMinutes = offsetStr.substring(3, 5).toInt()
            val offsetSign = if (offsetStr[0] == '+') 1 else -1
            val totalOffsetMinutes = offsetSign * (offsetHours * 60 + offsetMinutes)

            // Créer l'instant et le convertir en timezone Paris
            val instant = Instant.ofEpochMilli(timestampMs)
            val zonedDateTime = ZonedDateTime.ofInstant(instant, PARIS_ZONE)

            zonedDateTime.toLocalDateTime()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convertit un LocalDateTime en format ESF /Date(ms+offset)/
     *
     * @param dateTime La date à convertir
     * @return String au format /Date(ms+offset)/
     */
    fun toESFDate(dateTime: LocalDateTime): String {
        val zoned = dateTime.atZone(PARIS_ZONE)
        val millis = zoned.toInstant().toEpochMilli()
        val offset = zoned.offset.toString().replace(":", "")
        return "/Date($millis$offset)/"
    }

    /**
     * Formate une date pour l'affichage
     */
    fun formatForDisplay(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        return dateTime.format(formatter)
    }

    /**
     * Formate une date courte (juste l'heure)
     */
    fun formatTimeOnly(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return dateTime.format(formatter)
    }

    /**
     * Formate une date courte (juste le jour)
     */
    fun formatDateOnly(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return dateTime.format(formatter)
    }

    /**
     * Formate une date de manière relative (Aujourd'hui, Demain, etc.)
     */
    fun formatRelative(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate())

        return when {
            daysDiff == 0L -> "Aujourd'hui à ${formatTimeOnly(dateTime)}"
            daysDiff == 1L -> "Demain à ${formatTimeOnly(dateTime)}"
            daysDiff == -1L -> "Hier à ${formatTimeOnly(dateTime)}"
            daysDiff in 2..6 -> {
                val dayName = dateTime.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.FRENCH
                )
                "$dayName à ${formatTimeOnly(dateTime)}"
            }
            else -> formatForDisplay(dateTime)
        }
    }
}
