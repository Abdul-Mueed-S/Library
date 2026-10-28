package com.evu.library

import android.app.Activity
import android.util.TypedValue
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object UndoDeleteHelper {

    // Deletes immediately, then offers Undo for 6 seconds. Undo re-inserts the exact
    // same rows (same ids and flags), so nothing about the books changes.
    fun deleteWithUndo(
        activity: Activity,
        db: AppDatabase,
        scope: CoroutineScope,
        books: List<Book>,
        onChanged: () -> Unit
    ) {
        if (books.isEmpty()) return
        scope.launch {
            db.bookDao().deleteBooks(books)
            onChanged()

            val message = if (books.size == 1) "\"${books[0].title}\" deleted" else "${books.size} books deleted"
            val root = activity.findViewById<View>(android.R.id.content)
            val snackbar = Snackbar.make(root, message, 6000)
            snackbar.setAction("UNDO") {
                scope.launch {
                    db.bookDao().insertBooksWithId(books)
                    onChanged()
                }
            }
            snackbar.setBackgroundTint(resolveColor(activity, R.attr.appColorSurface))
            snackbar.setTextColor(resolveColor(activity, R.attr.appColorOnSurface))
            snackbar.setActionTextColor(resolveColor(activity, R.attr.appColorPrimary))
            snackbar.show()
        }
    }

    private fun resolveColor(activity: Activity, attr: Int): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}