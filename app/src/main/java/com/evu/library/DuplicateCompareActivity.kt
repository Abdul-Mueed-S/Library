package com.evu.library

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DuplicateCompareActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private var localId: Int = -1

    private fun resolveAttrColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duplicate_compare)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        localId = intent.getIntExtra("localId", -1)
        refresh()
    }

    private fun refresh() {
        val current = ImportHolder.books.find { it.localId == localId }
        if (current == null) {
            finish()
            return
        }

        val detailsParts = mutableListOf(current.title)
        current.author?.let { detailsParts.add("Author: $it") }
        current.edition?.let { detailsParts.add("Edition: $it") }
        current.year?.let { detailsParts.add("Year: $it") }
        current.isbn?.let { detailsParts.add("ISBN: $it") }
        findViewById<TextView>(R.id.currentDetailsText).text = detailsParts.joinToString("\n")

        findViewById<TextView>(R.id.currentEditButton).setOnClickListener {
            ImportEditDialogHelper.show(this, current) {
                lifecycleScope.launch {
                    val existing = db.bookDao().getAllBooks()
                    ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
                    refresh()
                }
            }
        }

        lifecycleScope.launch {
            val existingBooks = db.bookDao().getAllBooks()
            val existingMatches = existingBooks.filter {
                Utils.isDuplicate(it.title, it.edition, current.title, current.edition)
            }
            val batchMatches = ImportHolder.books.filter {
                it.localId != current.localId && Utils.isDuplicate(it.title, it.edition, current.title, current.edition)
            }

            val existingContainer = findViewById<LinearLayout>(R.id.existingMatchesContainer)
            existingContainer.removeAllViews()
            if (existingMatches.isEmpty()) {
                existingContainer.addView(makeEmptyLabel("No matches"))
            } else {
                for (match in existingMatches) {
                    existingContainer.addView(makeExistingMatchRow(match))
                }
            }

            val batchContainer = findViewById<LinearLayout>(R.id.batchMatchesContainer)
            batchContainer.removeAllViews()
            if (batchMatches.isEmpty()) {
                batchContainer.addView(makeEmptyLabel("No matches"))
            } else {
                for (match in batchMatches) {
                    batchContainer.addView(makeBatchMatchRow(match))
                }
            }
        }
    }

    private fun makeEmptyLabel(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.alpha = 0.6f
        tv.textSize = 13f
        return tv
    }

    private fun makeExistingMatchRow(book: Book): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, 12, 0, 12)

        val details = TextView(this)
        val parts = mutableListOf(book.title)
        book.edition?.let { parts.add("Edition: $it") }
        details.text = parts.joinToString(" • ")
        details.textSize = 14f
        details.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val editBtn = TextView(this)
        editBtn.text = "Edit"
        editBtn.setTextColor(resolveAttrColor(R.attr.appColorPrimary))
        editBtn.setPadding(16, 6, 16, 6)
        editBtn.setOnClickListener {
            BookDialogHelper.showAddOrEditBookDialog(this, db, lifecycleScope, book, book.categoryId) { primary, secondary ->
                Toast.makeText(this, primary, Toast.LENGTH_SHORT).show()
                lifecycleScope.launch { refresh() }
            }
        }

        val deleteBtn = TextView(this)
        deleteBtn.text = "Delete"
        deleteBtn.setTextColor(resolveAttrColor(R.attr.appColorDelete))
        deleteBtn.setPadding(16, 6, 16, 6)
        deleteBtn.setOnClickListener {
            AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setTitle("Delete Book")
                .setMessage("Delete \"${book.title}\" from your library? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        db.bookDao().deleteBook(book)
                        refresh()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        row.addView(details)
        row.addView(editBtn)
        row.addView(deleteBtn)
        return row
    }

    private fun makeBatchMatchRow(item: ImportedBook): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, 12, 0, 12)

        val details = TextView(this)
        val parts = mutableListOf(item.title)
        item.edition?.let { parts.add("Edition: $it") }
        details.text = parts.joinToString(" • ")
        details.textSize = 14f
        details.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val editBtn = TextView(this)
        editBtn.text = "Edit"
        editBtn.setTextColor(resolveAttrColor(R.attr.appColorPrimary))
        editBtn.setPadding(16, 6, 16, 6)
        editBtn.setOnClickListener {
            ImportEditDialogHelper.show(this, item) {
                lifecycleScope.launch {
                    val existing = db.bookDao().getAllBooks()
                    ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
                    refresh()
                }
            }
        }

        val removeBtn = TextView(this)
        removeBtn.text = "Remove"
        removeBtn.setTextColor(resolveAttrColor(R.attr.appColorDelete))
        removeBtn.setPadding(16, 6, 16, 6)
        removeBtn.setOnClickListener {
            AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setTitle("Remove Entry")
                .setMessage("Remove \"${item.title}\" from this import? This cannot be undone.")
                .setPositiveButton("Remove") { _, _ ->
                    ImportHolder.books.remove(item)
                    lifecycleScope.launch {
                        val existing = db.bookDao().getAllBooks()
                        ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
                        refresh()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        row.addView(details)
        row.addView(editBtn)
        row.addView(removeBtn)
        return row
    }
}