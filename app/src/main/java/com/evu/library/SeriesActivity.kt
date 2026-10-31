package com.evu.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

data class SeriesSummary(
    val name: String,
    val books: List<Book>,
    val ownedNumbers: Set<Int>,
    val total: Int
) {
    private val upper get() = maxOf(total, ownedNumbers.maxOrNull() ?: 0)
    val missing: List<Int> get() = (1..upper).filter { it !in ownedNumbers }
    val upperBound: Int get() = upper
}

class SeriesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: SeriesAdapter
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        statusText = findViewById(R.id.statusText)
        db = AppDatabase.getDatabase(this)

        adapter = SeriesAdapter(emptyList()) { showDetails(it) }
        val recycler = findViewById<RecyclerView>(R.id.seriesRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val summaries = db.bookDao().getAllBooks()
                .filter { !it.isDraft && !it.seriesName.isNullOrBlank() }
                .groupBy { it.seriesName!!.trim().lowercase() }
                .map { (_, books) ->
                    val owned = books.mapNotNull { it.seriesNumber }.toSet()
                    SeriesSummary(
                        name = books.first().seriesName!!.trim(),
                        books = books,
                        ownedNumbers = owned,
                        total = books.mapNotNull { it.seriesTotal }.maxOrNull() ?: 0
                    )
                }
                .sortedBy { it.name.lowercase() }
            adapter.update(summaries)
            statusText.text = "No series yet. Add a series name (and volume number) when adding or editing a book."
            statusText.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showDetails(series: SeriesSummary) {
        val byNumber = series.books.filter { it.seriesNumber != null }.associateBy { it.seriesNumber!! }
        val lines = mutableListOf<String>()
        for (n in 1..series.upperBound) {
            val book = byNumber[n]
            lines.add(
                if (book != null) "#$n  ${book.title}" + (if (book.copies > 1) "  (×${book.copies})" else "")
                else "#$n  — missing"
            )
        }
        series.books.filter { it.seriesNumber == null }.forEach { lines.add("Unnumbered  ${it.title}") }
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(series.name)
            .setMessage(lines.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }
}

class SeriesAdapter(
    private var items: List<SeriesSummary>,
    private val onClick: (SeriesSummary) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val row: View = view.findViewById(R.id.seriesCardRow)
        val name: TextView = view.findViewById(R.id.seriesNameText)
        val summary: TextView = view.findViewById(R.id.seriesSummaryText)
        val missing: TextView = view.findViewById(R.id.seriesMissingText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_series, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val series = items[position]
        holder.name.text = series.name
        val owned = series.ownedNumbers.size
        holder.summary.text = when {
            series.total > 0 && series.missing.isEmpty() -> "Complete — all ${series.total} volumes"
            series.total > 0 -> "Own $owned of ${series.total}"
            else -> "Own $owned (total unknown)"
        }
        if (series.missing.isNotEmpty()) {
            holder.missing.text = "Missing: " + series.missing.joinToString(", ") { "#$it" }
            holder.missing.visibility = View.VISIBLE
        } else {
            holder.missing.visibility = View.GONE
        }
        holder.row.setOnClickListener { onClick(series) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SeriesSummary>) {
        items = newItems
        notifyDataSetChanged()
    }
}