package com.evu.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Book::class, Category::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Add every future migration here, e.g. MIGRATION_3_4, MIGRATION_4_5.
        // NEVER bump the version number without adding the matching migration —
        // a missing migration crashes the app on that version, it does NOT
        // silently fall back to wiping data anymore.

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "library_database"
                )
                    // .addMigrations(MIGRATION_3_4, ...) — add here as they're written
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}