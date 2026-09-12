package com.evu.library

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CategoryDetailActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: BookAdapter
    private var categoryId: Int = -1
    private var categoryName: String = ""

    private fun resolveAttrColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        categoryId = intent.getIntExtra("categoryId", -1)
        categoryName = intent.getStringExtra("categoryName") ?: ""
        findViewById<android.widget.TextView>(R.id.categoryTitleText).text = "Category: $categoryName"

        db = AppDatabase.getDatabase(this)
        adapter = BookAdapter(emptyList()) { book -> showBookOptionsDialog(book) }
        val recyclerView = findViewById<RecyclerView>(R.id.categoryBookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        findViewById<android.widget.Button>(R.id.addNewBookButton).setOnClickListener {
            BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, null, categoryId) { primary, secondary ->
                Toast.makeText(this, primary, Toast.LENGTH_SHORT).show()
                if (secondary != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this, secondary, Toast.LENGTH_SHORT).show()
                    }, 1000)
                }
                loadBooks()
            }
        }

        findViewById<android.widget.Button>(R.id.addExistingBookButton).setOnClickListener {
            showAddExistingBookDialog()
        }

        loadBooks()
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            adapter.updateList(db.bookDao().getBooksByCategory(categoryId))
        }
    }

    private fun showAddExistingBookDialog() {
        lifecycleScope.launch {
            val allBooks = db.bookDao().getAllBooks().filter { it.categoryId != categoryId }
            val titles = allBooks.map { it.title }.toTypedArray()
            if (titles.isEmpty()) {
                Toast.makeText(this@CategoryDetailActivity, "No other books available", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(this@CategoryDetailActivity, R.style.AppDialogTheme)
                .setTitle("Add Existing Book")
                .setItems(titles) { _, which ->
                    val selected = allBooks[which]
                    lifecycleScope.launch {
                        db.bookDao().updateBook(selected.copy(categoryId = categoryId))
                        Toast.makeText(this@CategoryDetailActivity, "${selected.title} Added to $categoryName", Toast.LENGTH_SHORT).show()
                        loadBooks()
                    }
                }
                .show()
        }
    }

    private fun showBookOptionsDialog(book: Book) {
        val favLabel = if (book.isFavorite) "Remove from Favorites" else "Add to Favorites"
        val options = listOf("Edit", "Remove from Category", "Delete", favLabel)

        val dialogView = layoutInflater.inflate(R.layout.dialog_book_options, null)
        dialogView.findViewById<android.widget.TextView>(R.id.optionsTitle).text = book.title
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.optionsContainer)

        val dialog = AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setView(dialogView)
            .create()

        options.forEachIndexed { index, label ->
            val optionView = android.widget.TextView(this)
            optionView.text = label
            optionView.textSize = 16f
            optionView.setPadding(16, 24, 16, 24)
            optionView.setTextColor(resolveAttrColor(R.attr.appColorOnSurface))
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            optionView.setBackgroundResource(outValue.resourceId)
            optionView.isClickable = true
            optionView.isFocusable = true
            optionView.setOnClickListener {
                dialog.dismiss()
                when (index) {
                    0 -> BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, secondary ->
                        Toast.makeText(this, primary, Toast.LENGTH_SHORT).show()
                        if (secondary != null) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                Toast.makeText(this, secondary, Toast.LENGTH_SHORT).show()
                            }, 1000)
                        }
                        loadBooks()
                    }
                    1 -> lifecycleScope.launch {
                        db.bookDao().updateBook(book.copy(categoryId = null))
                        Toast.makeText(this@CategoryDetailActivity, "${book.title} Removed from Category", Toast.LENGTH_SHORT).show()
                        loadBooks()
                    }
                    2 -> lifecycleScope.launch {
                        db.bookDao().deleteBook(book)
                        Toast.makeText(this@CategoryDetailActivity, "${book.title} Deleted", Toast.LENGTH_SHORT).show()
                        loadBooks()
                    }
                    3 -> lifecycleScope.launch {
                        val newState = !book.isFavorite
                        db.bookDao().updateBook(book.copy(isFavorite = newState))
                        val msg = if (newState) "${book.title} Added to Favorites" else "${book.title} Removed from Favorites"
                        Toast.makeText(this@CategoryDetailActivity, msg, Toast.LENGTH_SHORT).show()
                        loadBooks()
                    }
                }
            }
            container.addView(optionView)
        }
        dialog.show()
    }
}