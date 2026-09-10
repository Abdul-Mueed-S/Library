package com.evu.library

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PendingDuplicatesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duplicates)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showOptions(book) }
        val recyclerView = findViewById<RecyclerView>(R.id.duplicatesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        load()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            adapter.updateList(db.bookDao().getFlaggedDuplicates())
        }
    }

    private fun showOptions(book: Book) {
        val options = arrayOf("Edit", "Delete", "Mark as Reviewed")
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(book.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, _ ->
                        lifecycleScope.launch {
                            db.bookDao().clearDuplicateFlag(book.id)
                            Toast.makeText(this@PendingDuplicatesActivity, primary, Toast.LENGTH_SHORT).show()
                            load()
                        }
                    }
                    1 -> AlertDialog.Builder(this, R.style.AppDialogTheme)
                        .setTitle("Delete Book")
                        .setMessage("Delete \"${book.title}\"? This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                db.bookDao().deleteBook(book)
                                Toast.makeText(this@PendingDuplicatesActivity, "${book.title} Deleted", Toast.LENGTH_SHORT).show()
                                load()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    2 -> lifecycleScope.launch {
                        db.bookDao().clearDuplicateFlag(book.id)
                        Toast.makeText(this@PendingDuplicatesActivity, "${book.title} Marked as Reviewed", Toast.LENGTH_SHORT).show()
                        load()
                    }
                }
            }
            .show()
    }
}