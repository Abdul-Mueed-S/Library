package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CategoriesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        findViewById<android.widget.LinearLayout>(R.id.pinnedFavouritesRow).setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.pinnedDraftsRow).setOnClickListener {
            startActivity(Intent(this, DraftsActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.pinnedDuplicatesRow).setOnClickListener {
            startActivity(Intent(this, PendingDuplicatesActivity::class.java))
        }

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

    private fun loadCategories() {
        lifecycleScope.launch {
            adapter.updateList(db.categoryDao().getAllCategories())
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