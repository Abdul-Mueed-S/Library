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
        // One cached instance per db file name, so switching vaults doesn't
        // require destroying/rebuilding instances for vaults you switch back to.
        private val instances = mutableMapOf<String, AppDatabase>()

        // Add every future migration here, e.g. MIGRATION_3_4, MIGRATION_4_5.
        // NEVER bump the version number without adding the matching migration.

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            val activeVault = VaultManager.getActiveVault(context)
            return instances.getOrPut(activeVault.dbFileName) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    activeVault.dbFileName
                )
                    // .addMigrations(MIGRATION_3_4, ...) — add here as they're written
                    .build()
            }
        }

        // Call when switching vaults so subsequent getDatabase() calls check
        // VaultManager's current active vault instead of returning a stale cached one
        // for a db file name that's no longer active — the map lookup above already
        // handles this correctly, this function exists for explicit close-on-switch
        // to release file handles cleanly.
        fun closeInstance(dbFileName: String) {
            instances[dbFileName]?.close()
            instances.remove(dbFileName)
        }
    }
}