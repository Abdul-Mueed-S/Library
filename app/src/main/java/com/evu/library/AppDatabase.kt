package com.evu.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, Category::class], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val instances = mutableMapOf<String, AppDatabase>()

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                // Initialize existing categories' order to match creation order (by id).
                db.execSQL("UPDATE categories SET sortOrder = id")
            }
        }

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            val activeVault = VaultManager.getActiveVault(context)
            return instances.getOrPut(activeVault.dbFileName) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    activeVault.dbFileName
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
            }
        }

        @Synchronized
        fun getDatabaseForVault(context: Context, vault: LibraryVault): AppDatabase {
            return instances.getOrPut(vault.dbFileName) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    vault.dbFileName
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
            }
        }

        fun closeInstance(dbFileName: String) {
            instances[dbFileName]?.close()
            instances.remove(dbFileName)
        }
    }
}