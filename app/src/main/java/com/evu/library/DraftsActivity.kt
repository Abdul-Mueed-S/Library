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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DraftsActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter
    private var fullList: List<Book> = emptyList()

    private var isSearchExpanded = false
    private var searchFilterIndex = 0
    private var sortFilterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drafts)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showOptions(book) }
        val recyclerView = findViewById<RecyclerView>(R.id.draftsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

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

        load()
    }

    override fun onResume() {
        super.onResume()
        load()
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

    private fun load() {
        lifecycleScope.launch {
            fullList = db.bookDao().getDraftBooks()
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
    }

    private fun showOptions(book: Book) {
        val dialogTitle = if (book.flaggedDuplicate) "${book.title} (Also a Duplicate)" else book.title
        val options = arrayOf("Edit", "Delete", "Mark as Published (remove from Drafts)")
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(dialogTitle)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, _ ->
                        Toast.makeText(this@DraftsActivity, primary, Toast.LENGTH_SHORT).show()
                        load()
                    }
                    1 -> AlertDialog.Builder(this, R.style.AppDialogTheme)
                        .setTitle("Delete Book")
                        .setMessage("Delete \"${book.title}\"? This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                db.bookDao().deleteBook(book)
                                Toast.makeText(this@DraftsActivity, "${book.title} Deleted", Toast.LENGTH_SHORT).show()
                                load()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    2 -> lifecycleScope.launch {
                        db.bookDao().clearDraftFlag(book.id)
                        Toast.makeText(this@DraftsActivity, "${book.title} Marked as Published", Toast.LENGTH_SHORT).show()
                        load()
                    }
                }
            }
            .show()
    }
}