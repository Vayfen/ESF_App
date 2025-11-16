package com.esf.calendar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.esf.calendar.data.model.ESFEvent
import com.esf.calendar.util.Constants

/**
 * Base de données Room pour l'application ESF Calendar
 * Stocke les événements en local pour le mode hors ligne
 */
@Database(
    entities = [ESFEvent::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ESFDatabase : RoomDatabase() {

    abstract fun eventDao(): ESFEventDao

    companion object {
        @Volatile
        private var INSTANCE: ESFDatabase? = null

        /**
         * Récupère l'instance singleton de la base de données
         */
        fun getDatabase(context: Context): ESFDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ESFDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // En dev, on peut se permettre de perdre les données
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
