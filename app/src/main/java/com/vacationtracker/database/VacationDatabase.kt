package com.vacationtracker.database

import android.content.Context
import androidx.room.*
import com.vacationtracker.models.*

@Database(
    entities = [Trip::class, LocationPoint::class, Place::class, Photo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VacationDatabase : RoomDatabase() {
    
    abstract fun tripDao(): TripDao
    abstract fun locationDao(): LocationDao
    abstract fun placeDao(): PlaceDao
    abstract fun photoDao(): PhotoDao
    
    companion object {
        @Volatile
        private var INSTANCE: VacationDatabase? = null
        
        fun getDatabase(context: Context): VacationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VacationDatabase::class.java,
                    "vacation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}