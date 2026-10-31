package com.evu.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, Category::class], version = 6, exportSchema = true)
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
                db.execSQL("UPDATE categories SET sortOrder = id")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN copies INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE books ADD COLUMN lentTo TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN lentAt INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN dueAt INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN seriesName TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN seriesNumber INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN seriesTotal INTEGER")
            }
        }

        private fun build(context: Context, fileName: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, fileName)
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            val vault = VaultManager.getActiveVault(context)
            return instances.getOrPut(vault.dbFileName) { build(context, vault.dbFileName) }
        }

        @Synchronized
        fun getDatabaseForVault(context: Context, vault: LibraryVault): AppDatabase =
            instances.getOrPut(vault.dbFileName) { build(context, vault.dbFileName) }

        fun closeInstance(dbFileName: String) {
            instances[dbFileName]?.close()
            instances.remove(dbFileName)
        }
    }
}