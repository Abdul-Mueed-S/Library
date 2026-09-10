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
    var isDuplicate: Boolean = false
)

object ExportImportUtils {

    fun booksToJson(books: List<Book>, categoryNames: Map<Int, String>, includeCategories: Boolean): String {
        val array = JSONArray()
        for (book in books) {
            val obj = JSONObject()
            obj.put("title", book.title)
            obj.put("author", book.author ?: JSONObject.NULL)
            obj.put("edition", book.edition ?: JSONObject.NULL)
            obj.put("year", book.year ?: JSONObject.NULL)
            obj.put("isbn", book.isbn ?: JSONObject.NULL)
            obj.put("favorite", book.isFavorite)
            val catName = if (includeCategories) book.categoryId?.let { categoryNames[it] } else null
            obj.put("category", catName ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun booksToCsv(books: List<Book>, categoryNames: Map<Int, String>, includeCategories: Boolean): String {
        val sb = StringBuilder()
        sb.append("Title,Author,Edition,Year,ISBN,Favorite,Category\n")
        for (book in books) {
            val catName = if (includeCategories) book.categoryId?.let { categoryNames[it] } else null
            sb.append(csvEscape(book.title)).append(",")
            sb.append(csvEscape(book.author ?: "")).append(",")
            sb.append(csvEscape(book.edition ?: "")).append(",")
            sb.append(csvEscape(book.year ?: "")).append(",")
            sb.append(csvEscape(book.isbn ?: "")).append(",")
            sb.append(book.isFavorite).append(",")
            sb.append(csvEscape(catName ?: "")).append("\n")
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
                    categoryName = obj.optString("category", null).takeIf { it != "null" && it.isNotBlank() }
                )
            )
        }
        return result
    }

    fun parseCsv(content: String): List<ImportedBook> {
        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val result = mutableListOf<ImportedBook>()
        var idCounter = 0
        for (i in 1 until lines.size) {
            val fields = parseCsvLine(lines[i])
            if (fields.size < 7) continue
            if (fields[0].isBlank()) continue
            result.add(
                ImportedBook(
                    localId = idCounter++,
                    title = fields[0],
                    author = fields[1].ifBlank { null },
                    edition = fields[2].ifBlank { null },
                    year = fields[3].ifBlank { null },
                    isbn = fields[4].ifBlank { null },
                    favorite = fields[5].trim().equals("true", ignoreCase = true),
                    categoryName = fields[6].ifBlank { null }
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
}

