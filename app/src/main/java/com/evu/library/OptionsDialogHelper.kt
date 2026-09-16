package com.evu.library

import android.app.Activity
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

data class DialogOption(val iconRes: Int, val label: String)

object OptionsDialogHelper {

    fun show(activity: Activity, title: String, options: List<DialogOption>, onSelect: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_book_options, null)
        dialogView.findViewById<TextView>(R.id.optionsTitle).text = title
        val container = dialogView.findViewById<LinearLayout>(R.id.optionsContainer)

        val dialog = AlertDialog.Builder(activity, R.style.AppDialogTheme)
            .setView(dialogView)
            .create()

        val textColor = resolveAttrColor(activity, R.attr.appColorOnSurface)
        val rippleValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, rippleValue, true)

        options.forEachIndexed { index, option ->
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(16, 22, 16, 22)
            row.isClickable = true
            row.isFocusable = true
            row.setBackgroundResource(rippleValue.resourceId)

            val icon = ImageView(activity)
            icon.setImageResource(option.iconRes)
            icon.layoutParams = LinearLayout.LayoutParams(48, 48)
            icon.setColorFilter(textColor)

            val label = TextView(activity)
            label.text = option.label
            label.textSize = 16f
            label.setTextColor(textColor)
            val labelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            labelParams.marginStart = 28
            label.layoutParams = labelParams

            row.addView(icon)
            row.addView(label)
            row.setOnClickListener {
                dialog.dismiss()
                onSelect(index)
            }
            container.addView(row)
        }
        dialog.show()
    }

    private fun resolveAttrColor(activity: Activity, attr: Int): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}