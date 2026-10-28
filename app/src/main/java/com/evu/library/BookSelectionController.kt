package com.evu.library

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

// Bottom action bar for multi-select. Overlays the activity content, so no layout
// changes are needed on any screen that hosts a BookAdapter.
class BookSelectionController(
    private val activity: ComponentActivity,
    private val db: AppDatabase,
    private val adapter: BookAdapter,
    private val recyclerView: RecyclerView,
    private val onDataChanged: () -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private val textColor = resolveColor(R.attr.appColorOnSurface)
    private val accentColor = resolveColor(R.attr.appColorPrimary)
    private val countText = TextView(activity)
    private val bar: LinearLayout
    private var isShowing = false
    private val basePaddingBottom = recyclerView.paddingBottom
    private val baseClipToPadding = recyclerView.clipToPadding

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            adapter.clearSelection()
        }
    }

    init {
        bar = buildBar()
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
        adapter.onSelectionChanged = { count -> handleSelectionChanged(count) }
    }

    private fun dp(value: Int) = (value * density).toInt()

    private fun resolveColor(attr: Int): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun rippleRes(): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return typedValue.resourceId
    }

    private fun toast(message: String) =
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    private fun buildBar(): LinearLayout {
        val container = LinearLayout(activity)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundResource(R.drawable.selection_bar_bg)
        container.elevation = dp(12).toFloat()
        container.setPadding(dp(12), dp(8), dp(12), dp(8))
        container.isClickable = true // swallow touches so they don't reach the list underneath

        val topRow = LinearLayout(activity)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL

        val closeButton = TextView(activity)
        closeButton.text = "✕"
        closeButton.textSize = 18f
        closeButton.setTextColor(textColor)
        closeButton.gravity = Gravity.CENTER
        closeButton.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        closeButton.isClickable = true
        closeButton.setBackgroundResource(rippleRes())
        closeButton.setOnClickListener { adapter.clearSelection() }

        countText.textSize = 16f
        countText.setTextColor(textColor)
        countText.setTypeface(null, android.graphics.Typeface.BOLD)
        val countParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        countParams.marginStart = dp(8)
        countText.layoutParams = countParams

        val selectAllButton = TextView(activity)
        selectAllButton.text = "Select all"
        selectAllButton.textSize = 14f
        selectAllButton.setTextColor(accentColor)
        selectAllButton.setPadding(dp(12), dp(8), dp(12), dp(8))
        selectAllButton.isClickable = true
        selectAllButton.setBackgroundResource(rippleRes())
        selectAllButton.setOnClickListener { adapter.selectAll() }

        topRow.addView(closeButton)
        topRow.addView(countText)
        topRow.addView(selectAllButton)

        val actionRow = LinearLayout(activity)
        actionRow.orientation = LinearLayout.HORIZONTAL
        actionRow.addView(actionCell(android.R.drawable.btn_star_big_on, "Favourite") { favouriteSelected() })
        actionRow.addView(actionCell(android.R.drawable.ic_menu_agenda, "Draft") { draftSelected() })
        actionRow.addView(actionCell(android.R.drawable.ic_menu_directions, "Move") { moveSelected() })
        actionRow.addView(actionCell(android.R.drawable.ic_menu_delete, "Delete") { deleteSelected() })

        container.addView(topRow)
        container.addView(actionRow)
        container.visibility = View.GONE
        return container
    }

    private fun actionCell(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout {
        val cell = LinearLayout(activity)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER
        cell.setPadding(0, dp(8), 0, dp(8))
        cell.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cell.isClickable = true
        cell.setBackgroundResource(rippleRes())

        val icon = ImageView(activity)
        icon.setImageResource(iconRes)
        icon.setColorFilter(textColor)
        icon.layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))

        val text = TextView(activity)
        text.text = label
        text.textSize = 11f
        text.setTextColor(textColor)
        text.gravity = Gravity.CENTER
        val textParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textParams.topMargin = dp(2)
        text.layoutParams = textParams

        cell.addView(icon)
        cell.addView(text)
        cell.setOnClickListener { onClick() }
        return cell
    }

    private fun handleSelectionChanged(count: Int) {
        backCallback.isEnabled = count > 0
        if (count > 0) {
            countText.text = "$count selected"
            show()
        } else {
            hide()
        }
    }

    private fun show() {
        if (isShowing) return
        isShowing = true
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        if (bar.parent == null) {
            bar.translationY = dp(400).toFloat() // start off-screen so there's no one-frame flash
            content.addView(
                bar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                )
            )
        }
        bar.visibility = View.VISIBLE
        bar.post {
            bar.translationY = bar.height.toFloat()
            bar.animate().translationY(0f).setDuration(180).start()
            // Keep the last list item reachable above the bar.
            recyclerView.clipToPadding = false
            recyclerView.setPadding(
                recyclerView.paddingLeft, recyclerView.paddingTop,
                recyclerView.paddingRight, basePaddingBottom + bar.height
            )
        }
    }

    private fun hide() {
        if (!isShowing) return
        isShowing = false
        recyclerView.setPadding(
            recyclerView.paddingLeft, recyclerView.paddingTop,
            recyclerView.paddingRight, basePaddingBottom
        )
        recyclerView.clipToPadding = baseClipToPadding
        bar.animate().translationY(bar.height.toFloat()).setDuration(160).withEndAction {
            if (!isShowing) bar.visibility = View.GONE
        }.start()
    }

    private fun favouriteSelected() {
        val selected = adapter.getSelectedBooks()
        if (selected.isEmpty()) return
        val makeFavourite = selected.any { !it.isFavorite }
        activity.lifecycleScope.launch {
            db.bookDao().updateBooks(selected.map { it.copy(isFavorite = makeFavourite) })
            adapter.clearSelection()
            onDataChanged()
            toast(
                if (makeFavourite) "${selected.size} added to Favourites"
                else "${selected.size} removed from Favourites"
            )
        }
    }

    private fun draftSelected() {
        val selected = adapter.getSelectedBooks()
        if (selected.isEmpty()) return
        val makeDraft = selected.any { !it.isDraft }
        activity.lifecycleScope.launch {
            db.bookDao().updateBooks(selected.map { it.copy(isDraft = makeDraft) })
            adapter.clearSelection()
            onDataChanged()
            toast(
                if (makeDraft) "${selected.size} marked as Draft"
                else "${selected.size} unmarked as Draft"
            )
        }
    }

    private fun moveSelected() {
        val selected = adapter.getSelectedBooks()
        if (selected.isEmpty()) return
        activity.lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()
            val names = (listOf("No category") + categories.map { it.name }).toTypedArray()
            AlertDialog.Builder(activity, R.style.AppDialogTheme)
                .setTitle("Move ${selected.size} to")
                .setItems(names) { _, which ->
                    val targetId = if (which == 0) null else categories[which - 1].id
                    activity.lifecycleScope.launch {
                        db.bookDao().updateBooks(selected.map { it.copy(categoryId = targetId) })
                        adapter.clearSelection()
                        onDataChanged()
                        toast("${selected.size} moved to ${names[which]}")
                    }
                }
                .show()
        }
    }

    private fun deleteSelected() {
        val selected = adapter.getSelectedBooks()
        if (selected.isEmpty()) return
        adapter.clearSelection()
        UndoDeleteHelper.deleteWithUndo(activity, db, activity.lifecycleScope, selected, onDataChanged)
    }
}