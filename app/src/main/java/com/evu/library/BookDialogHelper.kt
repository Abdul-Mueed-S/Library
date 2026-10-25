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

    // Auto-saved drafts must survive the dialog/activity being torn down (back press,
    // rotation, app switch), so they run on their own scope instead of the caller's
    // lifecycleScope, which is cancelled when the activity is destroyed.
    private val draftScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class FormValues(
        val title: String,
        val author: String?,
        val edition: String?,
        val year: String?,
        val isbn: String?,
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
        var explicitlySaved = false          // Save or Draft button completed
        var discarded = false                // Discard button pressed
        var autoSavedBook: Book? = null      // row created/updated by auto-save, if any
        val saveMutex = Mutex()              // serialises auto-save, discard and explicit save

        fun readForm(): FormValues = FormValues(
            title = Utils.toTitleCase(titleInput.text.toString()),
            author = Utils.toTitleCase(authorInput.text.toString()).ifEmpty { null },
            edition = Utils.clean(editionInput.text.toString()).ifEmpty { null },
            year = Utils.clean(yearInput.text.toString()).ifEmpty { null },
            isbn = Utils.clean(isbnInput.text.toString()).ifEmpty { null },
            categoryId = selectedCategoryId
        )

        // Saves whatever has been typed as a draft when the dialog is left without
        // Save / Draft / Discard (back, tap outside, app switch, rotation).
        fun autoSaveDraft() {
            if (explicitlySaved || discarded) return
            // Never silently modify a published book — only new entries and existing drafts.
            if (existingBook != null && !existingBook.isDraft) return

            val form = readForm()
            val hasContent = form.title.isNotEmpty() || form.author != null ||
                    form.edition != null || form.year != null || form.isbn != null
            if (!hasContent) return

            draftScope.launch {
                saveMutex.withLock {
                    if (explicitlySaved || discarded) return@withLock

                    val baseline = autoSavedBook ?: existingBook
                    val titleWasBlank = form.title.isEmpty()
                    val title = if (titleWasBlank) UNTITLED_TITLE else form.title

                    val unchanged = baseline != null &&
                            baseline.title == title &&
                            baseline.author == form.author &&
                            baseline.edition == form.edition &&
                            baseline.year == form.year &&
                            baseline.isbn == form.isbn &&
                            baseline.categoryId == form.categoryId
                    if (unchanged) return@withLock

                    val rowId = baseline?.id ?: 0
                    val isDuplicate = !titleWasBlank && db.bookDao().getAllBooks().any {
                        it.id != rowId && Utils.isDuplicate(it.title, it.edition, title, form.edition)
                    }

                    val draft = Book(
                        id = rowId,
                        title = title,
                        author = form.author,
                        edition = form.edition,
                        year = form.year,
                        isbn = form.isbn,
                        isFavorite = baseline?.isFavorite ?: false,
                        categoryId = form.categoryId,
                        flaggedDuplicate = isDuplicate,
                        isDraft = true
                    )

                    autoSavedBook = if (baseline == null) {
                        val newId = db.bookDao().insertBook(draft).toInt()
                        draft.copy(id = newId)
                    } else {
                        db.bookDao().updateBook(draft)
                        draft
                    }

                    withContext(Dispatchers.Main) { onResult("Draft saved: $title", null) }
                }
            }
        }

        // Discard throws the typed work away. If auto-save already persisted something
        // (e.g. the user switched apps and came back), undo that too.
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
                // Reuse the auto-saved row (if any) so Save never creates a second copy.
                val rowId = existingBook?.id ?: autoSavedBook?.id
                val book = Book(
                    id = rowId ?: 0,
                    title = form.title,
                    author = form.author,
                    edition = form.edition,
                    year = form.year,
                    isbn = form.isbn,
                    isFavorite = existingBook?.isFavorite ?: autoSavedBook?.isFavorite ?: false,
                    categoryId = form.categoryId,
                    flaggedDuplicate = flagged,
                    isDraft = isDraft
                )
                if (rowId != null) {
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
            // "Draft" always makes it a draft. "Save" publishes a new entry, but keeps an
            // existing entry's draft state (editing a draft must not silently un-draft it).
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

        // Auto-save when the app leaves the foreground while the dialog is open
        // (app switch, screen off, rotation) — onStop always runs before the activity can be destroyed.
        val lifecycleOwner = activity as? LifecycleOwner
        val stopObserver = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_STOP) autoSaveDraft()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(stopObserver)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptSave(forceDraft = false)
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                attemptSave(forceDraft = true)
            }
        }

        // Fires for back press, tap outside, Discard and programmatic dismiss. Explicit
        // saves and Discard set their flags first, so autoSaveDraft() is a no-op for them.
        dialog.setOnDismissListener {
            lifecycleOwner?.lifecycle?.removeObserver(stopObserver)
            autoSaveDraft()
        }

        dialog.show()
    }
}