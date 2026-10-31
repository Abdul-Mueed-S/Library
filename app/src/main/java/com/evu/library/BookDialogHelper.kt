package com.evu.library

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object BookDialogHelper {

    private const val NEW_CATEGORY_OPTION = "+ New Category"
    private const val UNTITLED_TITLE = "Untitled"

    // Auto-saved drafts run on their own scope so they survive the activity being torn down.
    private val draftScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class FormValues(
        val title: String,
        val author: String?,
        val edition: String?,
        val year: String?,
        val isbn: String?,
        val copies: Int,
        val seriesName: String?,
        val seriesNumber: Int?,
        val seriesTotal: Int?,
        val categoryId: Int?
    )

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
        val copiesInput = dialogView.findViewById<EditText>(R.id.copiesInput)
        val seriesNameInput = dialogView.findViewById<EditText>(R.id.seriesNameInput)
        val seriesNumberInput = dialogView.findViewById<EditText>(R.id.seriesNumberInput)
        val seriesTotalInput = dialogView.findViewById<EditText>(R.id.seriesTotalInput)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)

        copiesInput.setText("1")
        if (existingBook != null) {
            titleInput.setText(existingBook.title)
            authorInput.setText(existingBook.author ?: "")
            editionInput.setText(existingBook.edition ?: "")
            yearInput.setText(existingBook.year ?: "")
            isbnInput.setText(existingBook.isbn ?: "")
            copiesInput.setText(existingBook.copies.toString())
            seriesNameInput.setText(existingBook.seriesName ?: "")
            seriesNumberInput.setText(existingBook.seriesNumber?.toString() ?: "")
            seriesTotalInput.setText(existingBook.seriesTotal?.toString() ?: "")
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
                                            val maxOrder = db.categoryDao().getMaxSortOrder()
                                            val newId = db.categoryDao().insertCategory(Category(name = name, sortOrder = maxOrder + 1)).toInt()
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

        // ---------- Draft / auto-save state ----------
        var explicitlySaved = false
        var discarded = false
        var autoSavedBook: Book? = null
        val saveMutex = Mutex()

        fun readForm(): FormValues = FormValues(
            title = Utils.toTitleCase(titleInput.text.toString()),
            author = Utils.toTitleCase(authorInput.text.toString()).ifEmpty { null },
            edition = Utils.clean(editionInput.text.toString()).ifEmpty { null },
            year = Utils.clean(yearInput.text.toString()).ifEmpty { null },
            isbn = Utils.clean(isbnInput.text.toString()).ifEmpty { null },
            copies = copiesInput.text.toString().trim().toIntOrNull()?.coerceIn(1, 9999) ?: 1,
            seriesName = Utils.toTitleCase(seriesNameInput.text.toString()).ifEmpty { null },
            seriesNumber = seriesNumberInput.text.toString().trim().toIntOrNull()?.takeIf { it > 0 },
            seriesTotal = seriesTotalInput.text.toString().trim().toIntOrNull()?.takeIf { it > 0 },
            categoryId = selectedCategoryId
        )

        // Copies the form onto the base row, so lending, favourite and other untouched fields survive an edit.
        fun compose(base: Book?, form: FormValues, title: String, isDraft: Boolean, flagged: Boolean): Book =
            (base ?: Book(title = title)).copy(
                id = base?.id ?: 0,
                title = title,
                author = form.author,
                edition = form.edition,
                year = form.year,
                isbn = form.isbn,
                categoryId = form.categoryId,
                copies = form.copies,
                seriesName = form.seriesName,
                seriesNumber = form.seriesNumber,
                seriesTotal = form.seriesTotal,
                flaggedDuplicate = flagged,
                isDraft = isDraft
            )

        fun autoSaveDraft() {
            if (explicitlySaved || discarded) return
            // Never silently modify a published book — only new entries and existing drafts.
            if (existingBook != null && !existingBook.isDraft) return

            val form = readForm()
            val hasContent = form.title.isNotEmpty() || form.author != null || form.edition != null ||
                    form.year != null || form.isbn != null || form.seriesName != null
            if (!hasContent) return

            draftScope.launch {
                saveMutex.withLock {
                    if (explicitlySaved || discarded) return@withLock

                    val baseline = autoSavedBook ?: existingBook
                    val titleWasBlank = form.title.isEmpty()
                    val title = if (titleWasBlank) UNTITLED_TITLE else form.title

                    val unchanged = baseline != null &&
                            baseline.title == title && baseline.author == form.author &&
                            baseline.edition == form.edition && baseline.year == form.year &&
                            baseline.isbn == form.isbn && baseline.categoryId == form.categoryId &&
                            baseline.copies == form.copies && baseline.seriesName == form.seriesName &&
                            baseline.seriesNumber == form.seriesNumber && baseline.seriesTotal == form.seriesTotal
                    if (unchanged) return@withLock

                    val rowId = baseline?.id ?: 0
                    val isDuplicate = !titleWasBlank && db.bookDao().getAllBooks().any {
                        it.id != rowId && Utils.isDuplicate(it.title, it.edition, title, form.edition)
                    }

                    val draft = compose(baseline, form, title, isDraft = true, flagged = isDuplicate)
                    autoSavedBook = if (baseline == null) {
                        draft.copy(id = db.bookDao().insertBook(draft).toInt())
                    } else {
                        db.bookDao().updateBook(draft)
                        draft
                    }

                    withContext(Dispatchers.Main) { onResult("Draft saved: $title", null) }
                }
            }
        }

        fun discardWork() {
            discarded = true
            draftScope.launch {
                saveMutex.withLock {
                    val saved = autoSavedBook ?: return@withLock
                    if (existingBook == null) {
                        db.bookDao().deleteBook(saved)
                    } else {
                        db.bookDao().updateBook(existingBook)
                    }
                    autoSavedBook = null
                    withContext(Dispatchers.Main) { onResult("Discarded", null) }
                }
            }
        }

        suspend fun persistExplicit(form: FormValues, isDraft: Boolean, flagged: Boolean) {
            saveMutex.withLock {
                explicitlySaved = true
                val base = existingBook ?: autoSavedBook
                val book = compose(base, form, form.title, isDraft, flagged)
                if (base != null) {
                    db.bookDao().updateBook(book)
                } else {
                    db.bookDao().insertBook(book)
                }
            }

            val title = form.title
            if (existingBook == null) {
                when {
                    isDraft -> onResult("$title Saved as Draft", null)
                    newCategoryCreatedThisSession -> onResult("Book Added", "Book Added to $selectedCategoryName")
                    form.categoryId != null -> onResult("$title Added to $selectedCategoryName", null)
                    else -> onResult("$title Added", null)
                }
            } else {
                onResult(if (isDraft) "$title Edited (still a Draft)" else "$title Edited", null)
            }
        }

        val dialog = AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle(if (existingBook == null) "Add Book" else "Edit Book")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Discard") { _, _ -> discardWork() }
            .setNeutralButton("Draft", null)
            .create()

        fun attemptSave(forceDraft: Boolean) {
            val form = readForm()
            if (form.title.isEmpty()) {
                titleInput.error = "Title is required"
                return
            }
            val isDraft = if (forceDraft) true else (existingBook?.isDraft ?: false)

            scope.launch {
                val rowId = saveMutex.withLock { existingBook?.id ?: autoSavedBook?.id ?: 0 }
                val hasDuplicate = db.bookDao().getAllBooks().any {
                    it.id != rowId && Utils.isDuplicate(it.title, it.edition, form.title, form.edition)
                }
                if (hasDuplicate) {
                    AlertDialog.Builder(activity, R.style.AppDialogTheme)
                        .setTitle("Possible Duplicate")
                        .setMessage("This looks like a duplicate of a book already in your library. Save anyway?")
                        .setPositiveButton("Save Anyway") { _, _ ->
                            scope.launch {
                                persistExplicit(form, isDraft, flagged = true)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    persistExplicit(form, isDraft, flagged = false)
                    dialog.dismiss()
                }
            }
        }

        val lifecycleOwner = activity as? LifecycleOwner
        val stopObserver = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_STOP) autoSaveDraft()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(stopObserver)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { attemptSave(forceDraft = false) }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { attemptSave(forceDraft = true) }
        }

        dialog.setOnDismissListener {
            lifecycleOwner?.lifecycle?.removeObserver(stopObserver)
            autoSaveDraft()
        }

        dialog.show()
    }
}