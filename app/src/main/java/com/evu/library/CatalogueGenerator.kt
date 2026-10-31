package com.evu.library

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Builds an A4 catalogue PDF entirely on the device (no library, no internet).
object CatalogueGenerator {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val ROW_H = 15f

    private class Column(val title: String, val x: Float, val width: Float)

    private val columns = listOf(
        Column("#", 36f, 22f),
        Column("Title", 60f, 165f),
        Column("Author", 229f, 105f),
        Column("Ed. / Year", 338f, 55f),
        Column("ISBN", 397f, 62f),
        Column("Qty", 463f, 22f),
        Column("Category / Status", 489f, 70f)
    )

    private fun fit(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > width) end--
        return text.substring(0, end) + "…"
    }

    fun write(out: OutputStream, libraryName: String, books: List<Book>, categoryNames: Map<Int, String>) {
        val titlePaint = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val subPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt(); isAntiAlias = true }
        val headPaint = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 9f; isAntiAlias = true }
        val linePaint = Paint().apply { color = 0xFF999999.toInt(); strokeWidth = 0.6f }

        val rows = books.sortedBy { it.title.lowercase() }
        val doc = PdfDocument()
        var pageNumber = 0
        var index = 0
        val dateText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

        do {
            pageNumber++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            val canvas = page.canvas
            var y = MARGIN + 12f

            if (pageNumber == 1) {
                canvas.drawText("Library catalogue", MARGIN, y, titlePaint)
                y += 16f
                canvas.drawText("$libraryName  •  ${books.size} entries  •  $dateText", MARGIN, y, subPaint)
                y += 22f
            }

            columns.forEach { canvas.drawText(it.title, it.x, y, headPaint) }
            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
            y += ROW_H - 3f

            while (index < rows.size && y < PAGE_H - MARGIN - 14f) {
                val book = rows[index]
                val status = book.lentTo?.let { "Lent: $it" } ?: (book.categoryId?.let { categoryNames[it] } ?: "")
                val cells = listOf(
                    (index + 1).toString(),
                    book.title,
                    book.author ?: "",
                    listOfNotNull(book.edition, book.year).joinToString(" / "),
                    book.isbn ?: "",
                    book.copies.toString(),
                    status
                )
                columns.forEachIndexed { i, col -> canvas.drawText(fit(cells[i], bodyPaint, col.width), col.x, y, bodyPaint) }
                y += ROW_H
                index++
            }

            canvas.drawText("Page $pageNumber", PAGE_W / 2f - 18f, PAGE_H - MARGIN / 2f, subPaint)
            doc.finishPage(page)
        } while (index < rows.size)

        doc.writeTo(out)
        doc.close()
    }
}