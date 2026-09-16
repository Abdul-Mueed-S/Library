package com.evu.library

data class FullBackupCategory(val id: Int, val name: String, val sortOrder: Int = 0)

data class FullBackupBook(
    val id: Int,
    val title: String,
    val author: String?,
    val edition: String?,
    val year: String?,
    val isbn: String?,
    val isFavorite: Boolean,
    val categoryId: Int?,
    val flaggedDuplicate: Boolean,
    val isDraft: Boolean = false,
    val libraryId: Int? = null // placeholder for future multi-library support; always null until that schema exists
)

data class FullBackupData(
    val categories: List<FullBackupCategory>,
    val books: List<FullBackupBook>
)