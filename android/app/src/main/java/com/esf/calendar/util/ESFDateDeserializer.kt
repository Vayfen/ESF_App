package com.esf.calendar.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDateTime

/**
 * Deserializer personnalisé Gson pour les dates Microsoft JSON
 * Convertit automatiquement /Date(ms+offset)/ en LocalDateTime
 */
class ESFDateDeserializer : JsonDeserializer<LocalDateTime> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        val dateString = json?.asString ?: return null
        return DateParser.parseESFDate(dateString)
    }
}
