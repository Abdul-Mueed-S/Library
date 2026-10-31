package com.evu.library

import android.app.Activity
import android.util.TypedValue
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object UndoDeleteHelper {

    fun deleteWithUndo(
        activity: Activity,
        db: AppDatabase,
        scope: CoroutineScope,
        books: List<Book>,
        onChanged: () -> Unit
    ) {
        if (books.isEmpty()) return
        val ids = books.map { it.id }
        scope.launch {
            db.bookDao().trashBooks(ids, System.currentTimeMillis())
            onChanged()

            val message = if (books.size == 1) "\"${books[0].title}\" moved to Trash" else "${books.size} books moved to Trash"
            val snackbar = Snackbar.make(activity.findViewById<View>(android.R.id.content), message, 6000)
            snackbar.setAction("UNDO") {
                scope.launch {
                    db.bookDao().restoreFromTrash(ids)
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