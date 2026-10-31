package com.evu.library

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TrashActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_book_list
    override val recyclerViewId = R.id.bookRecyclerView
    override val selectionEnabled = false

    override fun onSetup() {
        findViewById<TextView>(R.id.listTitleText).text = "Trash"
        val emptyButton = findViewById<Button>(R.id.listActionButton)
        emptyButton.text = "Empty Trash"
        emptyButton.visibility = View.VISIBLE
        emptyButton.setOnClickListener { confirmEmpty() }
    }

    override suspend fun loadBooks() = db.bookDao().getTrashedBooks()

    override fun dialogTitle(book: Book): String {
        val deletedAt = book.deletedAt ?: return book.title
        val daysGone = ((System.currentTimeMillis() - deletedAt) / TrashPurgeWorker.DAY_MS).toInt()
        val daysLeft = (TrashPurgeWorker.RETENTION_DAYS - daysGone).toInt().coerceAtLeast(0)
        return "${book.title} (auto-deletes in $daysLeft days)"
    }

    override fun buildActions(book: Book) = listOf(
        actions.restoreFromTrash(book), actions.deleteForever(book)
    )

    private fun confirmEmpty() {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Empty Trash")
            .setMessage("Permanently delete everything in Trash? This cannot be undone.")
            .setPositiveButton("Empty") { _, _ ->
                lifecycleScope.launch {
                    db.bookDao().emptyTrash()
                    Toast.makeText(this@TrashActivity, "Trash emptied", Toast.LENGTH_SHORT).show()
                    reload()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}