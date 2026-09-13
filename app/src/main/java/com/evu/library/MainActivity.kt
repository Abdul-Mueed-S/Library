package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.evu.library.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter
    private lateinit var chipAdapter: ChipAdapter

    private var searchFilterIndex = 0
    private var sortFilterIndex = 0
    private var isSearchExpanded = false

    private var chips: List<ChipItem> = listOf(ChipItem.All, ChipItem.AddNew)
    private var selectedChipIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showBookOptionsDialog(book) }
        binding.bookRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.bookRecyclerView.adapter = adapter
        binding.bookRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        chipAdapter = ChipAdapter(chips, selectedChipIndex) { index, chip -> onChipSelected(index, chip) }
        binding.chipRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.chipRecyclerView.adapter = chipAdapter

        setupSpinners()

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            val itemId = item.itemId

            Handler(Looper.getMainLooper()).postDelayed({
                when (itemId) {
                    R.id.nav_home -> { }
                    R.id.nav_categories -> startActivity(Intent(this, CategoriesActivity::class.java))
                    R.id.nav_favourites -> startActivity(Intent(this, FavouritesActivity::class.java))
                    R.id.nav_drafts -> startActivity(Intent(this, DraftsActivity::class.java))
                    R.id.nav_duplicates -> startActivity(Intent(this, PendingDuplicatesActivity::class.java))
                    R.id.nav_export -> {
                        val intent = Intent(this, ImportExportActivity::class.java)
                        intent.putExtra("mode", "export")
                        startActivity(intent)
                    }
                    R.id.nav_import -> {
                        val intent = Intent(this, ImportExportActivity::class.java)
                        intent.putExtra("mode", "import")
                        startActivity(intent)
                    }
                    R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                    else -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }, 150)

            true
        }

        binding.searchButton.setOnClickListener { toggleSearch() }

        binding.dropdownButton.setOnClickListener {
            binding.filterRow.visibility =
                if (binding.filterRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.addBookButton.setOnClickListener {
            val defaultCategoryId = (chips.getOrNull(selectedChipIndex) as? ChipItem.CategoryChip)?.category?.id
            BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, null, defaultCategoryId) { primary, secondary ->
                showSequencedToast(primary, secondary)
                refreshCurrentView()
            }
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isBlank()) refreshCurrentView() else loadSearchResults(query)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadChips()
        adapter.notifyDataSetChanged() // picks up numbered-list toggle changed in Settings
    }

    private fun loadChips() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()
            val newChips = mutableListOf<ChipItem>(ChipItem.All, ChipItem.Favourites)
            newChips.addAll(categories.map { ChipItem.CategoryChip(it) })
            newChips.add(ChipItem.AddNew)

            val previousSelected = chips.getOrNull(selectedChipIndex)
            chips = newChips
            selectedChipIndex = when (previousSelected) {
                is ChipItem.CategoryChip -> chips.indexOfFirst {
                    it is ChipItem.CategoryChip && it.category.id == previousSelected.category.id
                }.let { if (it == -1) 0 else it }
                is ChipItem.Favourites -> 1
                else -> 0
            }
            chipAdapter.updateChips(chips, selectedChipIndex)
            refreshCurrentView()
        }
    }

    private fun onChipSelected(index: Int, chip: ChipItem) {
        if (chip is ChipItem.AddNew) {
            showAddCategoryDialog()
            return
        }
        selectedChipIndex = index
        refreshCurrentView()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("New Category")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = Utils.toTitleCase(input.text.toString())
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val existing = db.categoryDao().findByName(name)
                        if (existing != null) {
                            Toast.makeText(this@MainActivity, "Category \"$name\" already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            db.categoryDao().insertCategory(Category(name = name))
                            Toast.makeText(this@MainActivity, "Category: $name Created", Toast.LENGTH_SHORT).show()
                            loadChips()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshCurrentView() {
        if (binding.searchBar.text.toString().isNotBlank()) {
            loadSearchResults(binding.searchBar.text.toString())
            return
        }
        lifecycleScope.launch {
            val chip = chips.getOrNull(selectedChipIndex)
            val results = when (chip) {
                is ChipItem.Favourites -> db.bookDao().getFavoriteBooks()
                is ChipItem.CategoryChip -> db.bookDao().getBooksByCategory(chip.category.id)
                else -> db.bookDao().getAllBooks()
            }
            adapter.updateList(applySort(results))
        }
    }

    private fun applySort(list: List<Book>): List<Book> {
        return when (sortFilterIndex) {
            0 -> list.sortedByDescending { it.id }
            1 -> list.sortedBy { it.id }
            2 -> list.sortedBy { it.title.lowercase() }
            3 -> list.sortedByDescending { it.title.lowercase() }
            else -> list
        }
    }

    private fun showSequencedToast(primary: String, secondary: String?) {
        Toast.makeText(this, primary, Toast.LENGTH_SHORT).show()
        if (secondary != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, secondary, Toast.LENGTH_SHORT).show()
            }, 1000)
        }
    }

    private fun toggleSearch() {
        isSearchExpanded = !isSearchExpanded
        binding.searchBar.visibility = if (isSearchExpanded) View.VISIBLE else View.INVISIBLE
        binding.dropdownButton.visibility = if (isSearchExpanded) View.VISIBLE else View.GONE
        if (!isSearchExpanded) {
            binding.searchBar.setText("")
            binding.filterRow.visibility = View.GONE
            refreshCurrentView()
        }
    }

    private fun setupSpinners() {
        binding.searchFilterSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Title", "ISBN", "Author")
        )
        binding.searchFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                searchFilterIndex = pos
                loadSearchResults(binding.searchBar.text.toString())
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.sortFilterSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Newest", "Oldest", "Title A-Z", "Title Z-A")
        )
        binding.sortFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sortFilterIndex = pos
                refreshCurrentView()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadSearchResults(query: String) {
        if (query.isBlank()) {
            refreshCurrentView()
            return
        }
        lifecycleScope.launch {
            val results = when (searchFilterIndex) {
                1 -> db.bookDao().searchByIsbn(query)
                2 -> db.bookDao().searchByAuthor(query)
                else -> db.bookDao().searchAny(query)
            }
            adapter.updateList(applySort(results))
        }
    }

    private fun showBookOptionsDialog(book: Book) {
        val favLabel = if (book.isFavorite) "Remove from Favorites" else "Add to Favorites"
        val draftLabel = if (book.isDraft) "Unmark as Draft" else "Mark as Draft"
        val options = arrayOf("Edit", "Delete", favLabel, draftLabel)

        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(book.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, secondary ->
                        showSequencedToast(primary, secondary)
                        refreshCurrentView()
                    }
                    1 -> confirmDelete(book)
                    2 -> toggleFavorite(book)
                    3 -> toggleDraft(book)
                }
            }
            .show()
    }

    private fun toggleDraft(book: Book) {
        lifecycleScope.launch {
            val newState = !book.isDraft
            db.bookDao().updateBook(book.copy(isDraft = newState))
            val msg = if (newState) "${book.title} Marked as Draft" else "${book.title} Unmarked as Draft"
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            refreshCurrentView()
        }
    }

    private fun confirmDelete(book: Book) {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Delete Book")
            .setMessage("Delete \"${book.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().deleteBook(book)
                    Toast.makeText(this@MainActivity, "${book.title} Deleted", Toast.LENGTH_SHORT).show()
                    refreshCurrentView()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFavorite(book: Book) {
        lifecycleScope.launch {
            val newState = !book.isFavorite
            db.bookDao().updateBook(book.copy(isFavorite = newState))
            val msg = if (newState) "${book.title} Added to Favorites" else "${book.title} Removed from Favorites"
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            refreshCurrentView()
        }
    }
}