package com.evu.library

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object SeriesDialogs {

    // Name + total volumes editor, shared by "+ New Series" in the entry dialog and the Series screen.
    fun showEditor(
        activity: Activity,
        existing: Series?,
        onCancel: () -> Unit = {},
        onSave: (name: String, totalVolumes: Int?) -> Unit
    ) {
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(pad, pad / 2, pad, 0)

        val nameInput = EditText(activity)
        nameInput.hint = "Series name"
        nameInput.setText(existing?.name ?: "")
        val totalInput = EditText(activity)
        totalInput.hint = "Total volumes"
        totalInput.inputType = InputType.TYPE_CLASS_NUMBER
        totalInput.setText(existing?.totalVolumes?.toString() ?: "")
        container.addView(nameInput)
        container.addView(totalInput)

        AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle(if (existing == null) "New Series" else "Edit Series")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val name = Utils.toTitleCase(nameInput.text.toString())
                if (name.isEmpty()) {
                    Toast.makeText(activity, "Enter a series name", Toast.LENGTH_SHORT).show()
                    onCancel()
                    return@setPositiveButton
                }
                onSave(name, totalInput.text.toString().trim().toIntOrNull()?.takeIf { it > 0 })
            }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }
}