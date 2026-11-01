package com.evu.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, Category::class, Series::class], version = 7, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun seriesDao(): SeriesDao

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

        // Series text columns become a real `series` table; books point at it with seriesId.
        // SQLite on older Androids can't drop columns, so `books` is rebuilt.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `series` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `totalVolumes` INTEGER)")
                db.execSQL(
                    "INSERT INTO series (name, totalVolumes) " +
                            "SELECT MIN(TRIM(seriesName)), MAX(seriesTotal) FROM books " +
                            "WHERE seriesName IS NOT NULL AND TRIM(seriesName) != '' GROUP BY LOWER(TRIM(seriesName))"
                )
                db.execSQL(
                    "CREATE TABLE `books_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                            "`author` TEXT, `edition` TEXT, `year` TEXT, `isbn` TEXT, `isFavorite` INTEGER NOT NULL, " +
                            "`categoryId` INTEGER, `flaggedDuplicate` INTEGER NOT NULL, `isDraft` INTEGER NOT NULL, " +
                            "`copies` INTEGER NOT NULL, `lentTo` TEXT, `lentAt` INTEGER, `dueAt` INTEGER, " +
                            "`deletedAt` INTEGER, `seriesId` INTEGER, `seriesNumber` INTEGER)"
                )
                db.execSQL(
                    "INSERT INTO books_new (id, title, author, edition, year, isbn, isFavorite, categoryId, flaggedDuplicate, " +
                            "isDraft, copies, lentTo, lentAt, dueAt, deletedAt, seriesId, seriesNumber) " +
                            "SELECT b.id, b.title, b.author, b.edition, b.year, b.isbn, b.isFavorite, b.categoryId, b.flaggedDuplicate, " +
                            "b.isDraft, b.copies, b.lentTo, b.lentAt, b.dueAt, b.deletedAt, " +
                            "(SELECT s.id FROM series s WHERE LOWER(s.name) = LOWER(TRIM(b.seriesName)) LIMIT 1), b.seriesNumber " +
                            "FROM books b"
                )
                db.execSQL("DROP TABLE books")
                db.execSQL("ALTER TABLE books_new RENAME TO books")
            }
        }

        private fun build(context: Context, fileName: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, fileName)
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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