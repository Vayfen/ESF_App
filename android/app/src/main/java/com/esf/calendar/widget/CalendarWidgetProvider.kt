package com.esf.calendar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.esf.calendar.R
import com.esf.calendar.data.repository.ESFRepository
import com.esf.calendar.util.DateParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Widget pour afficher les prochains événements ESF sur l'écran d'accueil
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val repository = ESFRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val upcomingEvents = repository.getUpcomingEvents(5).first()

                val views = RemoteViews(context.packageName, R.layout.calendar_widget)

                if (upcomingEvents.isEmpty()) {
                    views.setTextViewText(R.id.widget_content, "Aucun événement à venir")
                } else {
                    val text = buildString {
                        for (event in upcomingEvents.take(3)) {
                            event.startDateTime?.let { start ->
                                appendLine("${DateParser.formatRelative(start)}")
                                appendLine(event.getTitle())
                                appendLine()
                            }
                        }
                    }
                    views.setTextViewText(R.id.widget_content, text.trim())
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                e.printStackTrace()
                val views = RemoteViews(context.packageName, R.layout.calendar_widget)
                views.setTextViewText(R.id.widget_content, "Erreur de chargement")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
