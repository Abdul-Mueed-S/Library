package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ImportPreviewActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: ImportPreviewAdapter
    private lateinit var categoriesCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_preview)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)
        categoriesCheckbox = findViewById(R.id.importCategoriesCheckbox)
        val hasAnyCategory = ImportHolder.books.any { it.categoryName != null }
        categoriesCheckbox.visibility = if (hasAnyCategory) android.view.View.VISIBLE else android.view.View.GONE
        categoriesCheckbox.isChecked = ImportHolder.defaultIncludeCategories

        val recyclerView = findViewById<RecyclerView>(R.id.importRecyclerView)
        adapter = ImportPreviewAdapter(
            ImportHolder.books,
            includeCategoriesProvider = { categoriesCheckbox.isChecked },
            onEdit = { position -> showEditDialog(position) },
            onRemove = { position -> confirmRemove(position) },
            onDuplicateTagClick = { item ->
                val intent = Intent(this, DuplicateCompareActivity::class.java)
                intent.putExtra("localId", item.localId)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        categoriesCheckbox.setOnCheckedChangeListener { _, _ -> adapter.notifyDataSetChanged() }

        findViewById<android.widget.Button>(R.id.confirmImportButton).setOnClickListener {
            attemptImport()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateList(ImportHolder.books)
    }

    private fun showEditDialog(position: Int) {
        val item = ImportHolder.books[position]
        ImportEditDialogHelper.show(this, item) {
            lifecycleScope.launch {
                val existing = db.bookDao().getAllBooks()
                ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
                adapter.updateList(ImportHolder.books)
            }
        }
    }

    private fun confirmRemove(position: Int) {
        val item = ImportHolder.books[position]
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Remove Entry")
            .setMessage("Remove \"${item.title}\" from this import? This cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                ImportHolder.books.remove(item)
                lifecycleScope.launch {
                    val existing = db.bookDao().getAllBooks()
                    ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
                    adapter.updateList(ImportHolder.books)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun attemptImport() {
        val itemsToImport = ImportHolder.books.toList()
        if (itemsToImport.isEmpty()) {
            Toast.makeText(this, "Nothing to import", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val existing = db.bookDao().getAllBooks()
            ExportImportUtils.computeDuplicateFlags(ImportHolder.books, existing)
            adapter.updateList(ImportHolder.books)
            val duplicateCount = ImportHolder.books.count { it.isDuplicate }
            if (duplicateCount > 0) {
                AlertDialog.Builder(this@ImportPreviewActivity, R.style.AppDialogTheme)
                    .setTitle("Duplicates Found")
                    .setMessage("$duplicateCount ${if (duplicateCount == 1) "entry looks" else "entries look"} like duplicates. Proceed with import anyway?")
                    .setPositiveButton("Proceed") { _, _ -> performImport() }
                    .setNegativeButton("Review", null)
                    .show()
            } else {
                performImport()
            }
        }
    }

    private fun performImport() {
        val includeCategories = categoriesCheckbox.isChecked
        val itemsToImport = ImportHolder.books.toList()

        lifecycleScope.launch {
            var count = 0
            for (item in itemsToImport) {
                var categoryId: Int? = null
                if (includeCategories && item.categoryName != null) {
                    val existingCat = db.categoryDao().findByName(item.categoryName!!)
                    categoryId = existingCat?.id ?: db.categoryDao().insertCategory(Category(name = item.categoryName!!)).toInt()
                }
                db.bookDao().insertBook(
                    Book(
                        title = item.title,
                        author = item.author,
                        edition = item.edition,
                        year = item.year,
                        isbn = item.isbn,
                        isFavorite = false,
                        categoryId = categoryId,
                        flaggedDuplicate = item.isDuplicate
                    )
                )
                count++
            }
            ImportHolder.books = mutableListOf()
            Toast.makeText(this@ImportPreviewActivity, "$count books imported", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}