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
    val libraryId: Int? = null,
    val copies: Int = 1,
    val lentTo: String? = null,
    val lentAt: Long? = null,
    val dueAt: Long? = null,
    val deletedAt: Long? = null,
    val seriesName: String? = null,
    val seriesNumber: Int? = null,
    val seriesTotal: Int? = null
)

data class FullBackupData(
    val categories: List<FullBackupCategory>,
    val books: List<FullBackupBook>
)