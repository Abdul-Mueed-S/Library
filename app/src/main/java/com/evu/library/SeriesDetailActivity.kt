package com.evu.library

import android.view.View
import android.widget.Button
import android.widget.TextView

class SeriesDetailActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_book_list
    override val recyclerViewId = R.id.bookRecyclerView
    override val primarySortLabel = "Volume order"
    override val keepLoadOrder = true

    private val seriesId by lazy { intent.getIntExtra("seriesId", -1) }
    private val seriesName by lazy { intent.getStringExtra("seriesName") ?: "" }

    override fun onSetup() {
        findViewById<TextView>(R.id.listTitleText).text = "Series: $seriesName"
        val addButton = findViewById<Button>(R.id.listActionButton)
        addButton.text = "Add Book to Series"
        addButton.visibility = View.VISIBLE
        addButton.setOnClickListener { actions.addBook(null, seriesId) }
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
        actions.edit(book), actions.removeFromSeries(book), actions.delete(book),
        actions.favourite(book), actions.draft(book), actions.lend(book)
    )
}