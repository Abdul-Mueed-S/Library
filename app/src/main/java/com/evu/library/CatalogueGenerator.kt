package com.evu.library

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CatalogueColumn(val label: String, val weight: Float) {
    AUTHOR("Author", 2f), YEAR("Year", 0.7f), ISBN("ISBN", 1.5f), COPIES("Qty", 0.55f),
    CATEGORY("Category", 1.4f), SERIES("Series", 1.8f), STATUS("Status", 1.4f)
}

data class CatalogueOptions(
    val libraryIds: Set<String>,
    val categoryFilter: String?,   // null = all, FILTER_NONE = uncategorised, else a category name
    val seriesFilter: String?,     // null = all, FILTER_NONE = not in a series, else a series name
    val includeDrafts: Boolean,
    val includeDuplicates: Boolean,
    val onlyFavourites: Boolean,
    val onlyLent: Boolean,
    val groupBy: Int,              // 0 none, 1 category, 2 series
    val sortBy: Int,               // 0 title, 1 author, 2 newest first
    val columns: Set<CatalogueColumn>,
    val pagePerLibrary: Boolean
)

class CatalogueLibrary(
    val name: String,
    val books: List<Book>,
    val categoryNames: Map<Int, String>,
    val series: Map<Int, Series>
)

// Builds an A4 PDF on the device from the chosen libraries, filters, grouping and columns.
object CatalogueGenerator {
    const val FILTER_NONE = "::none::"
    private const val W = 595
    private const val H = 842
    private const val M = 36f
    private const val ROW = 15f

    private class Col(val title: String, val x: Float, val width: Float, val column: CatalogueColumn?)

    private fun fit(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > width) end--
        return text.substring(0, end) + "…"
    }

    private fun matches(book: Book, lib: CatalogueLibrary, o: CatalogueOptions): Boolean {
        if (!o.includeDrafts && book.isDraft) return false
        if (!o.includeDuplicates && book.flaggedDuplicate) return false
        if (o.onlyFavourites && !book.isFavorite) return false
        if (o.onlyLent && book.lentTo == null) return false
        o.categoryFilter?.let { f ->
            val name = book.categoryId?.let { lib.categoryNames[it] }
            if (f == FILTER_NONE) { if (book.categoryId != null) return false }
            else if (name == null || !name.equals(f, ignoreCase = true)) return false
        }
        o.seriesFilter?.let { f ->
            val name = book.seriesId?.let { lib.series[it]?.name }
            if (f == FILTER_NONE) { if (book.seriesId != null) return false }
            else if (name == null || !name.equals(f, ignoreCase = true)) return false
        }
        return true
    }

    private fun sorted(books: List<Book>, sortBy: Int): List<Book> = when (sortBy) {
        1 -> books.sortedBy { (it.author ?: "~").lowercase() }
        2 -> books.sortedByDescending { it.id }
        else -> books.sortedBy { it.title.lowercase() }
    }

    private fun cell(col: CatalogueColumn, b: Book, lib: CatalogueLibrary): String = when (col) {
        CatalogueColumn.AUTHOR -> b.author ?: ""
        CatalogueColumn.YEAR -> b.year ?: ""
        CatalogueColumn.ISBN -> b.isbn ?: ""
        CatalogueColumn.COPIES -> b.copies.toString()
        CatalogueColumn.CATEGORY -> b.categoryId?.let { lib.categoryNames[it] } ?: ""
        CatalogueColumn.SERIES -> b.seriesId?.let { id ->
            lib.series[id]?.let { s -> s.name + (b.seriesNumber?.let { " #$it" } ?: "") }
        } ?: ""
        CatalogueColumn.STATUS -> when {
            b.lentTo != null -> "Lent: ${b.lentTo}"
            b.isDraft -> "Draft"
            b.flaggedDuplicate -> "Duplicate"
            else -> ""
        }
    }

    private fun summary(o: CatalogueOptions): String {
        val parts = mutableListOf<String>()
        o.categoryFilter?.let { parts.add("Category: " + if (it == FILTER_NONE) "none" else it) }
        o.seriesFilter?.let { parts.add("Series: " + if (it == FILTER_NONE) "none" else it) }
        if (o.onlyFavourites) parts.add("Favourites only")
        if (o.onlyLent) parts.add("Lent out only")
        if (o.includeDrafts) parts.add("incl. drafts")
        if (o.includeDuplicates) parts.add("incl. duplicates")
        return parts.joinToString("  •  ")
    }

    fun write(out: OutputStream, libraries: List<CatalogueLibrary>, o: CatalogueOptions) {
        val titlePaint = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val subPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt(); isAntiAlias = true }
        val headPaint = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 9f; isAntiAlias = true }
        val libPaint = Paint().apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val groupPaint = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF333333.toInt(); isAntiAlias = true }
        val linePaint = Paint().apply { color = 0xFF999999.toInt(); strokeWidth = 0.6f }

        // ---- columns ----
        val chosen = CatalogueColumn.values().filter { it in o.columns }
        val numW = 24f
        val gap = 4f
        val avail = (W - 2 * M) - numW - gap * (chosen.size + 2)
        val totalWeight = 3f + chosen.sumOf { it.weight.toDouble() }.toFloat()
        val layout = mutableListOf<Col>()
        var x = M
        layout.add(Col("#", x, numW, null)); x += numW + gap
        val titleW = avail * 3f / totalWeight
        layout.add(Col("Title", x, titleW, null)); x += titleW + gap
        for (c in chosen) {
            val w = avail * c.weight / totalWeight
            layout.add(Col(c.label, x, w, c)); x += w + gap
        }

        // ---- select + order the books of every library ----
        val selected = libraries.map { lib -> lib to lib.books.filter { matches(it, lib, o) } }
        val totalBooks = selected.sumOf { it.second.size }
        val dateText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        val showLibraryHeading = libraries.size > 1 || o.pagePerLibrary

        // ---- paging helpers ----
        val doc = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        var pageNo = 0

        fun drawColumnHeader() {
            val c = canvas!!
            layout.forEach { c.drawText(it.title, it.x, y, headPaint) }
            y += 4f
            c.drawLine(M, y, W - M, y, linePaint)
            y += ROW - 3f
        }

        fun closePage() {
            page?.let {
                canvas!!.drawText("Page $pageNo", W / 2f - 18f, H - M / 2f, subPaint)
                doc.finishPage(it)
            }
        }

        fun newPage() {
            closePage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            canvas = page!!.canvas
            y = M + 12f
            if (pageNo == 1) {
                canvas!!.drawText("Library catalogue", M, y, titlePaint)
                y += 16f
                canvas!!.drawText("${libraries.joinToString(", ") { it.name }}  •  $totalBooks entries  •  $dateText", M, y, subPaint)
                y += 14f
                val filters = summary(o)
                if (filters.isNotEmpty()) {
                    canvas!!.drawText(filters, M, y, subPaint)
                    y += 14f
                }
                y += 8f
            }
            drawColumnHeader()
        }

        fun ensure(height: Float) {
            if (y + height > H - M - 14f) newPage()
        }

        newPage()

        selected.forEachIndexed { index, (lib, books) ->
            if (showLibraryHeading) {
                if (o.pagePerLibrary && index > 0) newPage()
                ensure(ROW * 3)
                canvas!!.drawText("Library: ${lib.name}  (${books.size} entries)", M, y, libPaint)
                y += ROW + 2f
            }
            if (books.isEmpty()) {
                ensure(ROW)
                canvas!!.drawText("No matching entries.", M, y, bodyPaint)
                y += ROW
                return@forEachIndexed
            }

            val groups: List<Pair<String?, List<Book>>> = when (o.groupBy) {
                1 -> books.groupBy { b -> b.categoryId?.let { lib.categoryNames[it] } ?: "No category" }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER).map { it.key to sorted(it.value, o.sortBy) }
                2 -> books.groupBy { b -> b.seriesId?.let { lib.series[it]?.name } ?: "Not in a series" }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER).map { (name, list) ->
                        name to list.sortedWith(compareBy({ it.seriesNumber ?: Int.MAX_VALUE }, { it.title.lowercase() }))
                    }
                else -> listOf(null to sorted(books, o.sortBy))
            }

            var number = 0
            for ((groupName, groupBooks) in groups) {
                if (groupName != null) {
                    ensure(ROW * 2)
                    val label = if (o.groupBy == 1) "Category" else "Series"
                    canvas!!.drawText("$label: $groupName  (${groupBooks.size})", M, y, groupPaint)
                    y += ROW
                }
                for (book in groupBooks) {
                    ensure(ROW)
                    number++
                    val texts = listOf(number.toString(), book.title) + chosen.map { cell(it, book, lib) }
                    layout.forEachIndexed { i, col ->
                        canvas!!.drawText(fit(texts[i], bodyPaint, col.width), col.x, y, bodyPaint)
                    }
                    y += ROW
                }
            }
            y += 6f
        }

        closePage()
        doc.writeTo(out)
        doc.close()
    }
}