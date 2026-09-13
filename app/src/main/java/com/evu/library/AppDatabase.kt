package com.evu.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, Category::class], version = 4, exportSchema = true)
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

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            val activeVault = VaultManager.getActiveVault(context)
            return instances.getOrPut(activeVault.dbFileName) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    activeVault.dbFileName
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
            }
        }

        fun closeInstance(dbFileName: String) {
            instances[dbFileName]?.close()
            instances.remove(dbFileName)
        }
    }
}