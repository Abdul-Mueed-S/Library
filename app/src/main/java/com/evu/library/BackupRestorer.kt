package com.evu.library

import androidx.room.withTransaction

object BackupRestorer {

    // Replaces the whole library in one transaction: if anything fails midway,
    // Room rolls back and the existing library is left untouched.
    suspend fun restoreIntoDatabase(db: AppDatabase, data: FullBackupData) {
        db.withTransaction {
            db.bookDao().deleteAllBooks()
            db.categoryDao().deleteAllCategories()
            db.seriesDao().deleteAllSeries()
            for (cat in data.categories) {
                db.categoryDao().insertCategoryWithId(Category(id = cat.id, name = cat.name, sortOrder = cat.sortOrder))
            }
            db.seriesDao().insertAllSeriesWithId(
                data.series.map { Series(id = it.id, name = it.name, totalVolumes = it.totalVolumes) }
            )
            db.bookDao().insertBooksWithId(
                data.books.map { b ->
                    Book(
                        id = b.id, title = b.title, author = b.author, edition = b.edition,
                        year = b.year, isbn = b.isbn, isFavorite = b.isFavorite,
                        categoryId = b.categoryId, flaggedDuplicate = b.flaggedDuplicate,
                        isDraft = b.isDraft, copies = b.copies, lentTo = b.lentTo,
                        lentAt = b.lentAt, dueAt = b.dueAt, deletedAt = b.deletedAt,
                        seriesId = b.seriesId, seriesNumber = b.seriesNumber
                    )
                }
            )
        }
    }
}