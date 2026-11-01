package com.evu.library

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CategoryDetailActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_category_detail
    override val recyclerViewId = R.id.categoryBookRecyclerView

    private val categoryId by lazy { intent.getIntExtra("categoryId", -1) }
    private val categoryName by lazy { intent.getStringExtra("categoryName") ?: "" }

    override fun onSetup() {
        findViewById<TextView>(R.id.categoryTitleText).text = "Category: $categoryName"
        findViewById<Button>(R.id.addNewBookButton).setOnClickListener { actions.addBook(categoryId) }
        findViewById<Button>(R.id.addExistingBookButton).setOnClickListener { showAddExistingBookDialog() }
    }

    override suspend fun loadBooks() =
        db.bookDao().getBooksByCategory(categoryId).filter { !it.isDraft && !it.flaggedDuplicate }

    override fun buildActions(book: Book) = listOf(
        actions.edit(book), actions.removeFromCategory(book), actions.delete(book),
        actions.favourite(book), actions.moveToSeries(book), actions.draft(book), actions.lend(book)
    )

    private fun showAddExistingBookDialog() {
        lifecycleScope.launch {
            val others = db.bookDao().getAllBooks().filter { it.categoryId != categoryId }
            if (others.isEmpty()) {
                Toast.makeText(this@CategoryDetailActivity, "No other books available", Toast.LENGTH_SHORT).show()
                return@launch
            }
            AlertDialog.Builder(this@CategoryDetailActivity, R.style.AppDialogTheme)
                .setTitle("Add Existing Book")
                .setItems(others.map { it.title }.toTypedArray()) { _, which ->
                    val selected = others[which]
                    lifecycleScope.launch {
                        db.bookDao().updateBook(selected.copy(categoryId = categoryId))
                        Toast.makeText(this@CategoryDetailActivity, "${selected.title} Added to $categoryName", Toast.LENGTH_SHORT).show()
                        reload()
                    }
                }
                .show()
        }
    }
}