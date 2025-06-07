package com.mostree.memorycard.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mymaterialapp.model.LearningCard
import com.example.mymaterialapp.model.LearningDeck

@Database(entities = [LearningDeck::class, LearningCard::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun learningDeckDao(): LearningDeckDao
    abstract fun learningCardDao(): LearningCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learning_app_database"
                )
                // Optional: Add migrations if schema changes in the future
                // .addMigrations(MIGRATION_1_2, ...)
                // Optional: Prepopulate data if needed
                // .addCallback(object : Callback() { ... })
                .fallbackToDestructiveMigration() // If migrations are not set, it will clear DB on schema change (for dev)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
