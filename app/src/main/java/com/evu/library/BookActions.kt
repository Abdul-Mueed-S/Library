package com.evu.library

import android.app.DatePickerDialog
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

data class BookAction(val iconRes: Int, val label: String, val onClick: () -> Unit)

class BookActions(
    private val activity: ComponentActivity,
    private val db: AppDatabase,
    private val onChanged: () -> Unit
) {
    private val scope get() = activity.lifecycleScope

    fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    private fun sequenced(primary: String, secondary: String?) {
        toast(primary)
        if (secondary != null) Handler(Looper.getMainLooper()).postDelayed({ toast(secondary) }, 1000)
    }

    private fun update(newBook: Book, message: String) {
        scope.launch {
            db.bookDao().updateBook(newBook)
            toast(message)
            onChanged()
        }
    }

    fun show(title: String, actions: List<BookAction>) {
        OptionsDialogHelper.show(activity, title, actions.map { DialogOption(it.iconRes, it.label) }) { which ->
            actions[which].onClick()
        }
    }

    fun addBook(defaultCategoryId: Int?) {
        BookDialogHelper.showAddOrEditBookDialog(activity, db, scope, null, defaultCategoryId) { p, s ->
            sequenced(p, s)
            onChanged()
        }
    }

    fun edit(book: Book) = BookAction(android.R.drawable.ic_menu_edit, "Edit") {
        BookDialogHelper.showAddOrEditBookDialog(activity, db, scope, book, book.categoryId) { p, s ->
            sequenced(p, s)
            onChanged()
        }
    }

    fun delete(book: Book) = BookAction(android.R.drawable.ic_menu_delete, "Delete") {
        UndoDeleteHelper.deleteWithUndo(activity, db, scope, listOf(book), onChanged)
    }

    fun favourite(book: Book): BookAction {
        val on = book.isFavorite
        return BookAction(
            if (on) android.R.drawable.btn_star_big_off else android.R.drawable.btn_star_big_on,
            if (on) "Remove from Favourites" else "Add to Favourites"
        ) {
            update(
                book.copy(isFavorite = !on),
                if (on) "${book.title} Removed from Favourites" else "${book.title} Added to Favourites"
            )
        }
    }

    fun removeFromFavourites(book: Book) =
        BookAction(android.R.drawable.btn_star_big_off, "Remove from Favourites") {
            update(book.copy(isFavorite = false), "${book.title} Removed from Favourites")
        }

    fun draft(book: Book): BookAction {
        val on = book.isDraft
        return BookAction(android.R.drawable.ic_menu_agenda, if (on) "Unmark as Draft" else "Mark as Draft") {
            update(
                book.copy(isDraft = !on),
                if (on) "${book.title} Unmarked as Draft" else "${book.title} Marked as Draft"
            )
        }
    }

    fun publish(book: Book) =
        BookAction(android.R.drawable.ic_menu_revert, "Mark as Published (remove from Drafts)") {
            scope.launch {
                db.bookDao().clearDraftFlag(book.id)
                toast("${book.title} Marked as Published")
                onChanged()
            }
        }

    fun removeFromCategory(book: Book) =
        BookAction(android.R.drawable.ic_menu_close_clear_cancel, "Remove from Category") {
            update(book.copy(categoryId = null), "${book.title} Removed from Category")
        }

    fun lend(book: Book): BookAction =
        if (book.lentTo != null) {
            BookAction(android.R.drawable.ic_menu_revert, "Mark as Returned") {
                update(book.copy(lentTo = null, lentAt = null, dueAt = null), "${book.title} marked as returned")
            }
        } else {
            BookAction(android.R.drawable.ic_menu_share, "Lend…") { showLendDialog(book) }
        }

    fun restoreFromTrash(book: Book) = BookAction(android.R.drawable.ic_menu_revert, "Restore") {
        scope.launch {
            db.bookDao().restoreFromTrash(listOf(book.id))
            toast("${book.title} restored")
            onChanged()
        }
    }

    fun deleteForever(book: Book) = BookAction(android.R.drawable.ic_menu_delete, "Delete forever") {
        AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle("Delete Forever")
            .setMessage("Permanently delete \"${book.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                scope.launch {
                    db.bookDao().deleteBook(book)
                    toast("${book.title} deleted")
                    onChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLendDialog(book: Book) {
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(pad, pad / 2, pad, 0)

        val nameInput = EditText(activity)
        nameInput.hint = "Lent to (name)"

        var dueAt: Long? = null
        val dueView = TextView(activity)
        dueView.text = "Due date: none (tap to set)"
        dueView.textSize = 15f
        dueView.setPadding(0, pad / 2, 0, pad / 2)
        dueView.isClickable = true
        dueView.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(
                activity,
                { _, year, month, day ->
                    val picked = Calendar.getInstance()
                    picked.set(year, month, day, 23, 59, 0)
                    dueAt = picked.timeInMillis
                    dueView.text = "Due: ${DateFormat.format("MMM d, yyyy", picked.timeInMillis)}"
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        container.addView(nameInput)
        container.addView(dueView)

        AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle("Lend \"${book.title}\"")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val name = Utils.toTitleCase(nameInput.text.toString())
                if (name.isEmpty()) {
                    toast("Enter a name")
                    return@setPositiveButton
                }
                update(
                    book.copy(lentTo = name, lentAt = System.currentTimeMillis(), dueAt = dueAt),
                    "${book.title} lent to $name"
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}