package com.evu.library

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class FavouritesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter
    private lateinit var recyclerView: RecyclerView
    private var fullList: List<Book> = emptyList()

    private var isSearchExpanded = false
    private var searchFilterIndex = 0
    private var sortFilterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourites)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showBookOptionsDialog(book) }
        recyclerView = findViewById(R.id.favouritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupSpinners()

        findViewById<android.widget.ImageButton>(R.id.searchButton).setOnClickListener { toggleSearch() }
        findViewById<android.widget.ImageButton>(R.id.dropdownButton).setOnClickListener {
            val filterRow = findViewById<android.widget.LinearLayout>(R.id.filterRow)
            filterRow.visibility = if (filterRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        findViewById<EditText>(R.id.searchBar).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilterAndSort() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        loadFavourites()
    }

    override fun onResume() {
        super.onResume()
        loadFavourites()
    }

    private fun setupSpinners() {
        val searchSpinner = findViewById<android.widget.Spinner>(R.id.searchFilterSpinner)
        searchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Title", "ISBN", "Author"))
        searchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                searchFilterIndex = pos
                applyFilterAndSort()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val sortSpinner = findViewById<android.widget.Spinner>(R.id.sortFilterSpinner)
        sortSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Newest", "Oldest", "Title A-Z", "Title Z-A"))
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                sortFilterIndex = pos
                applyFilterAndSort()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun toggleSearch() {
        isSearchExpanded = !isSearchExpanded
        val searchBar = findViewById<EditText>(R.id.searchBar)
        val dropdownButton = findViewById<android.widget.ImageButton>(R.id.dropdownButton)
        searchBar.visibility = if (isSearchExpanded) View.VISIBLE else View.INVISIBLE
        dropdownButton.visibility = if (isSearchExpanded) View.VISIBLE else View.GONE
        if (!isSearchExpanded) {
            searchBar.setText("")
            findViewById<android.widget.LinearLayout>(R.id.filterRow).visibility = View.GONE
            applyFilterAndSort()
        }
    }

    private fun loadFavourites() {
        lifecycleScope.launch {
            fullList = db.bookDao().getFavoriteBooks().filter { !it.isDraft && !it.flaggedDuplicate }
            applyFilterAndSort()
        }
    }

    private fun applyFilterAndSort() {
        val query = findViewById<EditText>(R.id.searchBar).text.toString()
        val filtered = if (query.isBlank()) fullList else fullList.filter {
            when (searchFilterIndex) {
                1 -> it.isbn?.contains(query, ignoreCase = true) == true
                2 -> it.author?.contains(query, ignoreCase = true) == true
                else -> it.title.contains(query, ignoreCase = true)
            }
        }
        val sorted = when (sortFilterIndex) {
            1 -> filtered.sortedBy { it.id }
            2 -> filtered.sortedBy { it.title.lowercase() }
            3 -> filtered.sortedByDescending { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.id }
        }
        adapter.updateList(sorted)
        recyclerView.scheduleLayoutAnimation()
    }

    private fun showBookOptionsDialog(book: Book) {
        val favIcon = if (book.isFavorite) android.R.drawable.btn_star_big_off else android.R.drawable.btn_star_big_on
        val favLabel = "Remove from Favourites"
        val draftLabel = if (book.isDraft) "Unmark as Draft" else "Mark as Draft"

        val options = listOf(
            DialogOption(android.R.drawable.ic_menu_edit, "Edit"),
            DialogOption(android.R.drawable.ic_menu_delete, "Delete"),
            DialogOption(favIcon, favLabel),
            DialogOption(android.R.drawable.ic_menu_agenda, draftLabel)
        )

        OptionsDialogHelper.show(this, book.title, options) { which ->
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
                    Toast.makeText(this@FavouritesActivity, "${book.title} Removed from Favourites", Toast.LENGTH_SHORT).show()
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