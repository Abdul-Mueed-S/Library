package com.evu.library

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class FavouritesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter
    private var fullList: List<Book> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourites)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showBookOptionsDialog(book) }
        val recyclerView = findViewById<RecyclerView>(R.id.favouritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        findViewById<EditText>(R.id.searchEditText).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        loadFavourites()
    }

    override fun onResume() {
        super.onResume()
        loadFavourites()
    }

    private fun loadFavourites() {
        lifecycleScope.launch {
            fullList = db.bookDao().getFavoriteBooks()
            applyFilter(findViewById<EditText>(R.id.searchEditText).text.toString())
        }
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) fullList else fullList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.author?.contains(query, ignoreCase = true) == true ||
                    it.isbn?.contains(query, ignoreCase = true) == true
        }
        adapter.updateList(filtered)
    }

    private fun showBookOptionsDialog(book: Book) {
        val draftLabel = if (book.isDraft) "Unmark as Draft" else "Mark as Draft"
        val options = arrayOf("Edit", "Delete", "Remove from Favorites", draftLabel)

        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(book.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, secondary ->
                        Toast.makeText(this, primary, Toast.LENGTH_SHORT).show()
                        if (secondary != null) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                Toast.makeText(this, secondary, Toast.LENGTH_SHORT).show()
                            }, 1000)
                        }
                        loadFavourites()
                    }
                    1 -> confirmDelete(book)
                    2 -> lifecycleScope.launch {
                        db.bookDao().updateBook(book.copy(isFavorite = false))
                        Toast.makeText(this@FavouritesActivity, "${book.title} Removed from Favorites", Toast.LENGTH_SHORT).show()
                        loadFavourites()
                    }
                    3 -> lifecycleScope.launch {
                        val newState = !book.isDraft
                        db.bookDao().updateBook(book.copy(isDraft = newState))
                        val msg = if (newState) "${book.title} Marked as Draft" else "${book.title} Unmarked as Draft"
                        Toast.makeText(this@FavouritesActivity, msg, Toast.LENGTH_SHORT).show()
                        loadFavourites()
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(book: Book) {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Delete Book")
            .setMessage("Delete \"${book.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().deleteBook(book)
                    Toast.makeText(this@FavouritesActivity, "${book.title} Deleted", Toast.LENGTH_SHORT).show()
                    loadFavourites()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}