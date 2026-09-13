package com.evu.library

import org.json.JSONArray
import org.json.JSONObject

data class ImportedBook(
    val localId: Int,
    var title: String,
    var author: String?,
    var edition: String?,
    var year: String?,
    var isbn: String?,
    var favorite: Boolean,
    var categoryName: String?,
    var wasFlaggedDuplicate: Boolean = false,
    var isDuplicate: Boolean = false
)

object ExportImportUtils {

    fun booksToJson(
        books: List<Book>,
        categoryNames: Map<Int, String>,
        includeCategories: Boolean,
        includeFavorites: Boolean,
        includeDuplicateFlags: Boolean
    ): String {
        val array = JSONArray()
        for (book in books) {
            val obj = JSONObject()
            obj.put("title", book.title)
            obj.put("author", book.author ?: JSONObject.NULL)
            obj.put("edition", book.edition ?: JSONObject.NULL)
            obj.put("year", book.year ?: JSONObject.NULL)
            obj.put("isbn", book.isbn ?: JSONObject.NULL)
            if (includeFavorites) obj.put("favorite", book.isFavorite)
            if (includeDuplicateFlags) obj.put("flaggedDuplicate", book.flaggedDuplicate)
            val catName = if (includeCategories) book.categoryId?.let { categoryNames[it] } else null
            obj.put("category", catName ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun booksToCsv(
        books: List<Book>,
        categoryNames: Map<Int, String>,
        includeCategories: Boolean,
        includeFavorites: Boolean,
        includeDuplicateFlags: Boolean
    ): String {
        val sb = StringBuilder()
        val headers = mutableListOf("Title", "Author", "Edition", "Year", "ISBN")
        if (includeFavorites) headers.add("Favorite")
        if (includeDuplicateFlags) headers.add("FlaggedDuplicate")
        headers.add("Category")
        sb.append(headers.joinToString(",")).append("\n")

        for (book in books) {
            val catName = if (includeCategories) book.categoryId?.let { categoryNames[it] } else null
            val row = mutableListOf(
                csvEscape(book.title),
                csvEscape(book.author ?: ""),
                csvEscape(book.edition ?: ""),
                csvEscape(book.year ?: ""),
                csvEscape(book.isbn ?: "")
            )
            if (includeFavorites) row.add(book.isFavorite.toString())
            if (includeDuplicateFlags) row.add(book.flaggedDuplicate.toString())
            row.add(csvEscape(catName ?: ""))
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun parseJson(content: String): List<ImportedBook> {
        val result = mutableListOf<ImportedBook>()
        val array = JSONArray(content)
        var idCounter = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "")
            if (title.isEmpty()) continue
            result.add(
                ImportedBook(
                    localId = idCounter++,
                    title = title,
                    author = obj.optString("author", null).takeIf { it != "null" },
                    edition = obj.optString("edition", null).takeIf { it != "null" },
                    year = obj.optString("year", null).takeIf { it != "null" },
                    isbn = obj.optString("isbn", null).takeIf { it != "null" },
                    favorite = obj.optBoolean("favorite", false),
                    categoryName = obj.optString("category", null).takeIf { it != "null" && it.isNotBlank() },
                    wasFlaggedDuplicate = obj.optBoolean("flaggedDuplicate", false)
                )
            )
        }
        return result
    }

    fun parseCsv(content: String): List<ImportedBook> {
        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val header = parseCsvLine(lines[0]).map { it.trim() }
        val favoriteIdx = header.indexOf("Favorite")
        val duplicateIdx = header.indexOf("FlaggedDuplicate")
        val categoryIdx = header.indexOf("Category")

        val result = mutableListOf<ImportedBook>()
        var idCounter = 0
        for (i in 1 until lines.size) {
            val fields = parseCsvLine(lines[i])
            if (fields.isEmpty() || fields[0].isBlank()) continue
            result.add(
                ImportedBook(
                    localId = idCounter++,
                    title = fields.getOrElse(0) { "" },
                    author = fields.getOrNull(1)?.ifBlank { null },
                    edition = fields.getOrNull(2)?.ifBlank { null },
                    year = fields.getOrNull(3)?.ifBlank { null },
                    isbn = fields.getOrNull(4)?.ifBlank { null },
                    favorite = if (favoriteIdx >= 0) fields.getOrNull(favoriteIdx)?.trim()?.equals("true", ignoreCase = true) == true else false,
                    categoryName = if (categoryIdx >= 0) fields.getOrNull(categoryIdx)?.ifBlank { null } else null,
                    wasFlaggedDuplicate = if (duplicateIdx >= 0) fields.getOrNull(duplicateIdx)?.trim()?.equals("true", ignoreCase = true) == true else false
                )
            )
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }

    fun computeDuplicateFlags(items: MutableList<ImportedBook>, existingBooks: List<Book>) {
        for (item in items) {
            val matchesExisting = existingBooks.any { Utils.isDuplicate(it.title, it.edition, item.title, item.edition) }
            val matchesBatch = items.any { other ->
                other.localId != item.localId && Utils.isDuplicate(other.title, other.edition, item.title, item.edition)
            }
            item.isDuplicate = matchesExisting || matchesBatch
        }
    }

    fun fullBackupToJson(categories: List<Category>, books: List<Book>): String {
        val root = JSONObject()
        root.put("formatVersion", 1)

        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val bookArray = JSONArray()
        for (book in books) {
            val obj = JSONObject()
            obj.put("id", book.id)
            obj.put("title", book.title)
            obj.put("author", book.author ?: JSONObject.NULL)
            obj.put("edition", book.edition ?: JSONObject.NULL)
            obj.put("year", book.year ?: JSONObject.NULL)
            obj.put("isbn", book.isbn ?: JSONObject.NULL)
            obj.put("isFavorite", book.isFavorite)
            obj.put("categoryId", book.categoryId ?: JSONObject.NULL)
            obj.put("flaggedDuplicate", book.flaggedDuplicate)
            obj.put("isDraft", book.isDraft)
            obj.put("libraryId", JSONObject.NULL) // reserved for future multi-library support
            bookArray.put(obj)
        }
        root.put("books", bookArray)

        return root.toString(2)
    }

    fun parseFullBackupJson(content: String): FullBackupData? {
        return try {
            val root = JSONObject(content)
            val catArray = root.optJSONArray("categories") ?: JSONArray()
            val categories = mutableListOf<FullBackupCategory>()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categories.add(FullBackupCategory(obj.getInt("id"), obj.getString("name")))
            }

            val bookArray = root.optJSONArray("books") ?: JSONArray()
            val books = mutableListOf<FullBackupBook>()
            for (i in 0 until bookArray.length()) {
                val obj = bookArray.getJSONObject(i)
                books.add(
                    FullBackupBook(
                        id = obj.getInt("id"),
                        title = obj.getString("title"),
                        author = obj.optString("author", null).takeIf { it != "null" },
                        edition = obj.optString("edition", null).takeIf { it != "null" },
                        year = obj.optString("year", null).takeIf { it != "null" },
                        isbn = obj.optString("isbn", null).takeIf { it != "null" },
                        isFavorite = obj.optBoolean("isFavorite", false),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getInt("categoryId"),
                        flaggedDuplicate = obj.optBoolean("flaggedDuplicate", false),
                        isDraft = obj.optBoolean("isDraft", false),
                        libraryId = if (obj.has("libraryId") && !obj.isNull("libraryId")) obj.getInt("libraryId") else null
                    )
                )
            }
            FullBackupData(categories, books)
        } catch (e: Exception) {
            null
        }
    }
}