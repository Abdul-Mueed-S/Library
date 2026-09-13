package com.evu.library

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val author: String? = null,
    val edition: String? = null,
    val year: String? = null,
    val isbn: String? = null,
    val isFavorite: Boolean = false,
    val categoryId: Int? = null,
    val flaggedDuplicate: Boolean = false,
    val isDraft: Boolean = false
)