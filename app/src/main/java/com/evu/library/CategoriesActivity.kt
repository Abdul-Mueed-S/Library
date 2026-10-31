package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CategoriesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private val badges = mutableMapOf<String, TextView>()

    private data class Pinned(val label: String, val target: Class<*>, val badgeKey: String? = null)

    private val pinnedRows = listOf(
        Pinned("Favourites", FavouritesActivity::class.java),
        Pinned("Drafts", DraftsActivity::class.java, "drafts"),
        Pinned("Duplicates", PendingDuplicatesActivity::class.java),
        Pinned("Lent Out", LentOutActivity::class.java, "lent"),
        Pinned("Series", SeriesActivity::class.java),
        Pinned("Trash", TrashActivity::class.java, "trash")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        buildPinnedRows()

        db = AppDatabase.getDatabase(this)
        val recyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        adapter = CategoryAdapter(
            emptyList(),
            onClick = { category ->
                val intent = Intent(this, CategoryDetailActivity::class.java)
                intent.putExtra("categoryId", category.id)
                intent.putExtra("categoryName", category.name)
                startActivity(intent)
            },
            onLongClick = { category -> confirmDeleteCategory(category) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<android.widget.Button>(R.id.addCategoryButton).setOnClickListener {
            showAddCategoryDialog()
        }

        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun buildPinnedRows() {
        val container = findViewById<LinearLayout>(R.id.pinnedContainer)
        pinnedRows.forEachIndexed { index, pinned ->
            val row = layoutInflater.inflate(R.layout.item_pinned_row, container, false)
            row.findViewById<TextView>(R.id.pinnedLabel).text = pinned.label
            row.findViewById<View>(R.id.pinnedDivider).visibility =
                if (index == pinnedRows.lastIndex) View.GONE else View.VISIBLE
            pinned.badgeKey?.let { badges[it] = row.findViewById(R.id.pinnedBadge) }
            row.findViewById<View>(R.id.pinnedRow).setOnClickListener {
                startActivity(Intent(this, pinned.target))
            }
            container.addView(row)
        }
    }

    private fun setBadge(key: String, count: Int) {
        badges[key]?.let {
            it.text = count.toString()
            it.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val counts = db.bookDao().getCategoryCounts().associate { it.categoryId to it.bookCount }
            adapter.updateList(db.categoryDao().getAllCategories(), counts)

            setBadge("drafts", db.bookDao().getDraftCount())
            setBadge("lent", db.bookDao().getLentCount())
            setBadge("trash", db.bookDao().getTrashCount())
        }
    }

    private fun confirmDeleteCategory(category: Category) {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(category.name)
            .setMessage("Rename or delete this category?")
            .setNegativeButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().clearCategoryFromBooks(category.id)
                    db.categoryDao().deleteCategory(category)
                    Toast.makeText(this@CategoriesActivity, "Category: ${category.name} Deleted", Toast.LENGTH_SHORT).show()
                    loadCategories()
                }
            }
            .setPositiveButton("Edit") { _, _ ->
                showEditCategoryDialog(category)
            }
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val input = EditText(this)
        input.setText(category.name)
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Edit Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = Utils.toTitleCase(input.text.toString())
                if (newName.isEmpty()) return@setPositiveButton
                lifecycleScope.launch {
                    val existing = db.categoryDao().findByName(newName)
                    if (existing != null && existing.id != category.id) {
                        Toast.makeText(this@CategoriesActivity, "Category \"$newName\" already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        db.categoryDao().updateCategory(category.copy(name = newName))
                        Toast.makeText(this@CategoriesActivity, "Category: $newName Updated", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                            Toast.makeText(this@CategoriesActivity, "Category \"$name\" already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            val maxOrder = db.categoryDao().getMaxSortOrder()
                            db.categoryDao().insertCategory(Category(name = name, sortOrder = maxOrder + 1))
                            Toast.makeText(this@CategoriesActivity, "Category: $name Created", Toast.LENGTH_SHORT).show()
                            loadCategories()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}