package com.evu.library

import androidx.room.withTransaction

object BackupRestorer {

    // Replaces the whole library in one transaction: if anything fails midway,
    // Room rolls back and the existing library is left untouched.
    suspend fun restoreIntoDatabase(db: AppDatabase, data: FullBackupData) {
        db.withTransaction {
            db.bookDao().deleteAllBooks()
            db.categoryDao().deleteAllCategories()
            for (cat in data.categories) {
                db.categoryDao().insertCategoryWithId(
                    Category(id = cat.id, name = cat.name, sortOrder = cat.sortOrder)
                )
            }
            db.bookDao().insertBooksWithId(
                data.books.map { book ->
                    Book(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        edition = book.edition,
                        year = book.year,
                        isbn = book.isbn,
                        isFavorite = book.isFavorite,
                        categoryId = book.categoryId,
                        flaggedDuplicate = book.flaggedDuplicate,
                        isDraft = book.isDraft
                    )
                }
            )
        }
    }
}