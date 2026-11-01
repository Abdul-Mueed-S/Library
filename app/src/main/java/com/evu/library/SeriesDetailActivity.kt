package com.evu.library

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SeriesDetailActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_book_list
    override val recyclerViewId = R.id.bookRecyclerView
    override val primarySortLabel = "Volume order"
    override val keepLoadOrder = true

    private val seriesId by lazy { intent.getIntExtra("seriesId", -1) }
    private val seriesName by lazy { intent.getStringExtra("seriesName") ?: "" }

    override fun onSetup() {
        findViewById<TextView>(R.id.listTitleText).text = "Series: $seriesName"
        findViewById<Button>(R.id.listActionButton).apply {
            text = "Add New Book"
            visibility = View.VISIBLE
            setOnClickListener { actions.addBook(null, seriesId) }
        }
        findViewById<Button>(R.id.listActionButton2).apply {
            text = "Add Existing Book"
            visibility = View.VISIBLE
            setOnClickListener { showAddExistingDialog() }
        }
    }

    override suspend fun loadBooks(): List<Book> {
        val books = db.bookDao().getBooksBySeries(seriesId)
            .filter { !it.isDraft && !it.flaggedDuplicate }
            .sortedWith(compareBy({ it.seriesNumber ?: Int.MAX_VALUE }, { it.title.lowercase() }))
        db.seriesDao().getById(seriesId)?.let { series ->
            val row = SeriesRow(series, books.size, books.mapNotNull { it.seriesNumber }.toSet())
            val subtitle = findViewById<TextView>(R.id.listSubtitleText)
            subtitle.text = listOfNotNull(row.summary, row.missingText).joinToString("  •  ")
            subtitle.visibility = View.VISIBLE
        }
        return books
    }

    override fun buildActions(book: Book) = listOf(
        actions.edit(book), actions.removeFromSeries(book), actions.moveToSeries(book),
        actions.delete(book), actions.favourite(book), actions.draft(book), actions.lend(book)
    )

    // Pick a book that is already in the library, then give it a volume number (mandatory).
    private fun showAddExistingDialog() {
        lifecycleScope.launch {
            val candidates = db.bookDao().getAllBooks()
                .filter { it.seriesId != seriesId && !it.isDraft }
                .sortedBy { it.title.lowercase() }
            if (candidates.isEmpty()) {
                Toast.makeText(this@SeriesDetailActivity, "No other books available", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(this@SeriesDetailActivity, R.style.AppDialogTheme)
                .setTitle("Add existing book to $seriesName")
                .setItems(candidates.map { it.title }.toTypedArray()) { _, which ->
                    val book = candidates[which]
                    lifecycleScope.launch {
                        val nextVolume = (db.bookDao().getBooksBySeries(seriesId).mapNotNull { it.seriesNumber }.maxOrNull() ?: 0) + 1
                        SeriesDialogs.askVolume(
                            this@SeriesDetailActivity, "Volume number",
                            "A volume number is required for books in a series.", nextVolume
                        ) { number ->
                            lifecycleScope.launch {
                                db.bookDao().updateBook(book.copy(seriesId = seriesId, seriesNumber = number))
                                Toast.makeText(this@SeriesDetailActivity, "${book.title} added as #$number", Toast.LENGTH_SHORT).show()
                                reload()
                            }
                        }
                    }
                }
                .show()
        }
    }
}