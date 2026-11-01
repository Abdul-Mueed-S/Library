package com.evu.library

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object SeriesDialogs {

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

    // Volume numbers are mandatory for books in a series: the dialog won't close until one is entered.
    fun askVolume(
        activity: Activity,
        title: String,
        message: String,
        default: Int?,
        onDone: (Int) -> Unit
    ) {
        val pad = (20 * activity.resources.displayMetrics.density).toInt()
        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(pad, pad / 2, pad, 0)

        val info = TextView(activity)
        info.text = message
        info.textSize = 13f
        info.alpha = 0.8f
        val input = EditText(activity)
        input.hint = "Volume number *"
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(default?.toString() ?: "")
        container.addView(info)
        container.addView(input)

        val dialog = AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val number = input.text.toString().trim().toIntOrNull()
                if (number == null || number < 1) {
                    input.error = "Volume number is required"
                    return@setOnClickListener
                }
                dialog.dismiss()
                onDone(number)
            }
        }
        dialog.show()
    }
}