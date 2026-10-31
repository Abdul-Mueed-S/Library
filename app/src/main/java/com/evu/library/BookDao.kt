package com.evu.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

data class CategoryCount(val categoryId: Int, val bookCount: Int)

@Dao
interface BookDao {

    @Insert
    suspend fun insertBook(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookWithId(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooksWithId(books: List<Book>)

    @Update
    suspend fun updateBook(book: Book)

    @Update
    suspend fun updateBooks(books: List<Book>)

    @Query("SELECT * FROM books WHERE deletedAt IS NULL")
    suspend fun getAllBooks(): List<Book>

    // Everything including trash — used by snapshots.
    @Query("SELECT * FROM books")
    suspend fun getEveryBook(): List<Book>

    @Query("SELECT * FROM books WHERE deletedAt IS NULL ORDER BY id DESC LIMIT 3")
    suspend fun getRecentBooks(): List<Book>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId AND deletedAt IS NULL")
    suspend fun getBooksByCategory(categoryId: Int): List<Book>

    @Query("SELECT * FROM books WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%') AND deletedAt IS NULL")
    suspend fun searchAny(query: String): List<Book>

    @Query("SELECT * FROM books WHERE author LIKE '%' || :query || '%' AND deletedAt IS NULL")
    suspend fun searchByAuthor(query: String): List<Book>

    @Query("SELECT * FROM books WHERE isbn LIKE '%' || :query || '%' AND deletedAt IS NULL")
    suspend fun searchByIsbn(query: String): List<Book>

    @Query("SELECT * FROM books WHERE title = :title AND author IS :author AND isbn IS :isbn AND deletedAt IS NULL LIMIT 1")
    suspend fun findDuplicate(title: String, author: String?, isbn: String?): Book?

    @Query("UPDATE books SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromBooks(categoryId: Int)

    @Query("SELECT * FROM books WHERE isFavorite = 1 AND deletedAt IS NULL")
    suspend fun getFavoriteBooks(): List<Book>

    @Query("SELECT * FROM books WHERE flaggedDuplicate = 1 AND deletedAt IS NULL")
    suspend fun getFlaggedDuplicates(): List<Book>

    @Query("UPDATE books SET flaggedDuplicate = 0 WHERE id = :id")
    suspend fun clearDuplicateFlag(id: Int)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("SELECT * FROM books WHERE isDraft = 1 AND deletedAt IS NULL")
    suspend fun getDraftBooks(): List<Book>

    @Query("SELECT COUNT(*) FROM books WHERE isDraft = 1 AND deletedAt IS NULL")
    suspend fun getDraftCount(): Int

    @Query("UPDATE books SET isDraft = 0 WHERE id = :id")
    suspend fun clearDraftFlag(id: Int)

    @Query("SELECT * FROM books WHERE lentTo IS NOT NULL AND deletedAt IS NULL")
    suspend fun getLentBooks(): List<Book>

    @Query("SELECT COUNT(*) FROM books WHERE lentTo IS NOT NULL AND deletedAt IS NULL")
    suspend fun getLentCount(): Int

    @Query("SELECT categoryId, COUNT(*) AS bookCount FROM books WHERE categoryId IS NOT NULL AND isDraft = 0 AND flaggedDuplicate = 0 AND deletedAt IS NULL GROUP BY categoryId")
    suspend fun getCategoryCounts(): List<CategoryCount>

    // ---- Trash ----
    @Query("UPDATE books SET deletedAt = :now WHERE id IN (:ids)")
    suspend fun trashBooks(ids: List<Int>, now: Long)

    @Query("UPDATE books SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreFromTrash(ids: List<Int>)

    @Query("SELECT * FROM books WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getTrashedBooks(): List<Book>

    @Query("SELECT COUNT(*) FROM books WHERE deletedAt IS NOT NULL")
    suspend fun getTrashCount(): Int

    @Query("DELETE FROM books WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrashedBefore(cutoff: Long)

    @Query("DELETE FROM books WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Delete
    suspend fun deleteBook(book: Book)

    @Delete
    suspend fun deleteBooks(books: List<Book>)
}