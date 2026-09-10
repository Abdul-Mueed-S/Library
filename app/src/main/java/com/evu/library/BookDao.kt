package com.evu.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import androidx.room.Delete

@Dao
interface BookDao {

    @Insert
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<Book>

    @Query("SELECT * FROM books ORDER BY id DESC LIMIT 3")
    suspend fun getRecentBooks(): List<Book>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId")
    suspend fun getBooksByCategory(categoryId: Int): List<Book>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%'")
    suspend fun searchAny(query: String): List<Book>

    @Query("SELECT * FROM books WHERE author LIKE '%' || :query || '%'")
    suspend fun searchByAuthor(query: String): List<Book>

    @Query("SELECT * FROM books WHERE isbn LIKE '%' || :query || '%'")
    suspend fun searchByIsbn(query: String): List<Book>

    @Query("SELECT * FROM books WHERE title = :title AND author IS :author AND isbn IS :isbn LIMIT 1")
    suspend fun findDuplicate(title: String, author: String?, isbn: String?): Book?

    @Query("UPDATE books SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromBooks(categoryId: Int)

    @Query("SELECT * FROM books WHERE isFavorite = 1")
    suspend fun getFavoriteBooks(): List<Book>

    @Query("SELECT * FROM books WHERE flaggedDuplicate = 1")
    suspend fun getFlaggedDuplicates(): List<Book>

    @Query("UPDATE books SET flaggedDuplicate = 0 WHERE id = :id")
    suspend fun clearDuplicateFlag(id: Int)

    @Delete
    suspend fun deleteBook(book: Book)
}