package com.evu.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryWithId(category: Category)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Category?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun getMaxSortOrder(): Int

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}