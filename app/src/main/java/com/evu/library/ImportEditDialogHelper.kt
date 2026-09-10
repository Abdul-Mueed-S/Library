package com.evu.library

import android.app.Activity
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

object ImportEditDialogHelper {
    fun show(activity: Activity, item: ImportedBook, onSaved: () -> Unit) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_import, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTitleInput)
        val authorInput = dialogView.findViewById<EditText>(R.id.editAuthorInput)
        val editionInput = dialogView.findViewById<EditText>(R.id.editEditionInput)
        val yearInput = dialogView.findViewById<EditText>(R.id.editYearInput)
        val isbnInput = dialogView.findViewById<EditText>(R.id.editIsbnInput)
        val categoryInput = dialogView.findViewById<EditText>(R.id.editCategoryInput)

        titleInput.setText(item.title)
        authorInput.setText(item.author ?: "")
        editionInput.setText(item.edition ?: "")
        yearInput.setText(item.year ?: "")
        isbnInput.setText(item.isbn ?: "")
        categoryInput.setText(item.categoryName ?: "")

        AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle("Edit Entry")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = Utils.toTitleCase(titleInput.text.toString())
                if (title.isEmpty()) return@setPositiveButton
                item.title = title
                item.author = Utils.toTitleCase(authorInput.text.toString()).ifEmpty { null }
                item.edition = Utils.clean(editionInput.text.toString()).ifEmpty { null }
                item.year = Utils.clean(yearInput.text.toString()).ifEmpty { null }
                item.isbn = Utils.clean(isbnInput.text.toString()).ifEmpty { null }
                item.categoryName = Utils.toTitleCase(categoryInput.text.toString()).ifEmpty { null }
                onSaved()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}