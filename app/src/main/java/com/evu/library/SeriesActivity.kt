package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

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

        adapter = SeriesAdapter(
            emptyList(),
            onClick = { row ->
                val intent = Intent(this, SeriesDetailActivity::class.java)
                intent.putExtra("seriesId", row.series.id)
                intent.putExtra("seriesName", row.series.name)
                startActivity(intent)
            },
            onLongClick = { row -> showOptions(row.series) }
        )
        val recycler = findViewById<RecyclerView>(R.id.seriesRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.addSeriesButton).setOnClickListener {
            SeriesDialogs.showEditor(this, null) { name, total -> createSeries(name, total) }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val allSeries = db.seriesDao().getAllSeries()
            val booksBySeries = db.bookDao().getAllBooks()
                .filter { !it.isDraft && !it.flaggedDuplicate && it.seriesId != null }
                .groupBy { it.seriesId!! }
            adapter.update(
                allSeries.map { s ->
                    val books = booksBySeries[s.id].orEmpty()
                    SeriesRow(s, books.size, books.mapNotNull { it.seriesNumber }.toSet())
                }
            )
            statusText.text = "No series yet. Tap + to add one, or choose \"+ New Series\" while adding a book."
            statusText.visibility = if (allSeries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun createSeries(name: String, total: Int?) {
        lifecycleScope.launch {
            if (db.seriesDao().findByName(name) != null) {
                Toast.makeText(this@SeriesActivity, "Series \"$name\" already exists", Toast.LENGTH_SHORT).show()
            } else {
                db.seriesDao().insertSeries(Series(name = name, totalVolumes = total))
                Toast.makeText(this@SeriesActivity, "Series: $name Created", Toast.LENGTH_SHORT).show()
                load()
            }
        }
    }

    private fun showOptions(series: Series) {
        OptionsDialogHelper.show(
            this, series.name,
            listOf(
                DialogOption(android.R.drawable.ic_menu_edit, "Edit"),
                DialogOption(android.R.drawable.ic_menu_delete, "Delete series")
            )
        ) { which ->
            if (which == 0) editSeries(series) else confirmDelete(series)
        }
    }

    private fun editSeries(series: Series) {
        SeriesDialogs.showEditor(this, series) { name, total ->
            lifecycleScope.launch {
                val existing = db.seriesDao().findByName(name)
                if (existing != null && existing.id != series.id) {
                    Toast.makeText(this@SeriesActivity, "Series \"$name\" already exists", Toast.LENGTH_SHORT).show()
                } else {
                    db.seriesDao().updateSeries(series.copy(name = name, totalVolumes = total))
                    Toast.makeText(this@SeriesActivity, "Series: $name Updated", Toast.LENGTH_SHORT).show()
                    load()
                }
            }
        }
    }

    private fun confirmDelete(series: Series) {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Delete Series")
            .setMessage("Delete \"${series.name}\"? Its books stay in your library; they just leave the series.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().clearSeriesFromBooks(series.id)
                    db.seriesDao().deleteSeries(series)
                    Toast.makeText(this@SeriesActivity, "Series: ${series.name} Deleted", Toast.LENGTH_SHORT).show()
                    load()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}