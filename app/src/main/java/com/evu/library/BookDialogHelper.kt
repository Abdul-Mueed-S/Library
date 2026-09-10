package com.evu.library

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object BookDialogHelper {

    private const val NEW_CATEGORY_OPTION = "+ New Category"

    fun showAddOrEditBookDialog(
        activity: Activity,
        db: AppDatabase,
        scope: CoroutineScope,
        existingBook: Book?,
        defaultCategoryId: Int?,
        onResult: (primaryMessage: String, secondaryMessage: String?) -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_book, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val authorInput = dialogView.findViewById<EditText>(R.id.authorInput)
        val editionInput = dialogView.findViewById<EditText>(R.id.editionInput)
        val yearInput = dialogView.findViewById<EditText>(R.id.yearInput)
        val isbnInput = dialogView.findViewById<EditText>(R.id.isbnInput)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)

        if (existingBook != null) {
            titleInput.setText(existingBook.title)
            authorInput.setText(existingBook.author ?: "")
            editionInput.setText(existingBook.edition ?: "")
            yearInput.setText(existingBook.year ?: "")
            isbnInput.setText(existingBook.isbn ?: "")
        }

        isbnInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val formatted = Utils.formatIsbn(s.toString())
                if (formatted != s.toString()) {
                    isbnInput.setText(formatted)
                    isbnInput.setSelection(formatted.length)
                }
                isFormatting = false
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        var categories = listOf<Category>()
        var selectedCategoryId: Int? = defaultCategoryId
        var selectedCategoryName: String? = null
        var newCategoryCreatedThisSession = false

        fun refreshSpinner(selectId: Int?) {
            scope.launch {
                categories = db.categoryDao().getAllCategories()
                val names = mutableListOf("None")
                names.addAll(categories.map { it.name })
                names.add(NEW_CATEGORY_OPTION)
                categorySpinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, names)
                val index = when {
                    selectId == null -> 0
                    else -> categories.indexOfFirst { it.id == selectId }.let { if (it == -1) 0 else it + 1 }
                }
                categorySpinner.setSelection(index)
                selectedCategoryName = categories.find { it.id == selectId }?.name
            }
        }
        refreshSpinner(existingBook?.categoryId ?: defaultCategoryId)

        categorySpinner.post {
            categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    val selectedName = categorySpinner.selectedItem as? String ?: return
                    if (selectedName == NEW_CATEGORY_OPTION) {
                        val input = EditText(activity)
                        AlertDialog.Builder(activity, R.style.AppDialogTheme)
                            .setTitle("New Category")
                            .setView(input)
                            .setPositiveButton("Create") { _, _ ->
                                val name = Utils.toTitleCase(input.text.toString())
                                if (name.isNotEmpty()) {
                                    scope.launch {
                                        val existing = db.categoryDao().findByName(name)
                                        if (existing != null) {
                                            selectedCategoryId = existing.id
                                            selectedCategoryName = existing.name
                                            refreshSpinner(existing.id)
                                        } else {
                                            val newId = db.categoryDao().insertCategory(Category(name = name)).toInt()
                                            selectedCategoryId = newId
                                            selectedCategoryName = name
                                            newCategoryCreatedThisSession = true
                                            refreshSpinner(newId)
                                        }
                                    }
                                }
                            }
                            .setNegativeButton("Cancel") { _, _ -> refreshSpinner(selectedCategoryId) }
                            .show()
                    } else if (selectedName == "None") {
                        selectedCategoryId = null
                        selectedCategoryName = null
                    } else {
                        selectedCategoryId = categories.find { it.name == selectedName }?.id
                        selectedCategoryName = selectedName
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }

        val dialog = AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle(if (existingBook == null) "Add Book" else "Edit Book")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = Utils.toTitleCase(titleInput.text.toString())
                if (title.isEmpty()) {
                    titleInput.error = "Title is required"
                    return@setOnClickListener
                }
                val edition = Utils.clean(editionInput.text.toString()).ifEmpty { null }
                val book = Book(
                    id = existingBook?.id ?: 0,
                    title = title,
                    author = Utils.toTitleCase(authorInput.text.toString()).ifEmpty { null },
                    edition = edition,
                    year = Utils.clean(yearInput.text.toString()).ifEmpty { null },
                    isbn = Utils.clean(isbnInput.text.toString()).ifEmpty { null },
                    isFavorite = existingBook?.isFavorite ?: false,
                    categoryId = selectedCategoryId
                )

                scope.launch {
                    val allBooks = db.bookDao().getAllBooks()
                    val hasDuplicate = allBooks.any {
                        it.id != book.id && Utils.isDuplicate(it.title, it.edition, book.title, book.edition)
                    }
                    if (hasDuplicate) {
                        AlertDialog.Builder(activity, R.style.AppDialogTheme)
                            .setTitle("Possible Duplicate")
                            .setMessage("This looks like a duplicate of a book already in your library. Save anyway?")
                            .setPositiveButton("Save Anyway") { _, _ ->
                                val flaggedBook = book.copy(flaggedDuplicate = true)
                                scope.launch { saveBook(db, flaggedBook, existingBook, newCategoryCreatedThisSession, selectedCategoryId, selectedCategoryName, title, onResult) }
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        saveBook(db, book, existingBook, newCategoryCreatedThisSession, selectedCategoryId, selectedCategoryName, title, onResult)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }
    private suspend fun saveBook(
        db: AppDatabase,
        book: Book,
        existingBook: Book?,
        newCategoryCreated: Boolean,
        selectedCategoryId: Int?,
        selectedCategoryName: String?,
        title: String,
        onResult: (String, String?) -> Unit
    ) {
        if (existingBook == null) {
            db.bookDao().insertBook(book)
            if (newCategoryCreated) {
                onResult("Book Added", "Book Added to $selectedCategoryName")
            } else if (selectedCategoryId != null) {
                onResult("$title Added to $selectedCategoryName", null)
            } else {
                onResult("$title Added", null)
            }
        } else {
            db.bookDao().updateBook(book)
            onResult("$title Edited", null)
        }
    }
}