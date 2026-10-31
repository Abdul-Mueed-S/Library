package com.evu.library

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

abstract class BookListActivity : BaseActivity() {

    protected abstract val layoutRes: Int
    protected abstract val recyclerViewId: Int
    protected open val showDuplicateTag: Boolean = true
    protected open val selectionEnabled: Boolean = true

    protected abstract suspend fun loadBooks(): List<Book>
    protected abstract fun buildActions(book: Book): List<BookAction>
    protected open fun dialogTitle(book: Book): String = book.title
    protected open fun onSetup() {}

    protected lateinit var db: AppDatabase
    protected lateinit var actions: BookActions
    private lateinit var adapter: BookAdapter
    private lateinit var recyclerView: RecyclerView
    private var fullList: List<Book> = emptyList()

    private var isSearchExpanded = false
    private var searchFilterIndex = 0
    private var sortFilterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        actions = BookActions(this, db) { reload() }
        adapter = BookAdapter(emptyList(), showDuplicateTag) { book ->
            actions.show(dialogTitle(book), buildActions(book))
        }
        recyclerView = findViewById(recyclerViewId)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        if (selectionEnabled) BookSelectionController(this, db, adapter, recyclerView) { reload() }

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

        onSetup()
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    protected fun reload() {
        lifecycleScope.launch {
            fullList = loadBooks()
            applyFilterAndSort()
        }
    }

    private fun setupSpinners() {
        val searchSpinner = findViewById<android.widget.Spinner>(R.id.searchFilterSpinner)
        searchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Title", "ISBN", "Author"))
        searchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                searchFilterIndex = pos
                applyFilterAndSort()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val sortSpinner = findViewById<android.widget.Spinner>(R.id.sortFilterSpinner)
        sortSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Newest", "Oldest", "Title A-Z", "Title Z-A"))
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sortFilterIndex = pos
                applyFilterAndSort()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun toggleSearch() {
        isSearchExpanded = !isSearchExpanded
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.visibility = if (isSearchExpanded) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.dropdownButton).visibility = if (isSearchExpanded) View.VISIBLE else View.GONE
        if (!isSearchExpanded) {
            searchBar.setText("")
            findViewById<View>(R.id.filterRow).visibility = View.GONE
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
}