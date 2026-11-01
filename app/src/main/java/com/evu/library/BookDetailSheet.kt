package com.evu.library

import android.graphics.Color
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

object BookDetailSheet {

    fun show(activity: ComponentActivity, db: AppDatabase, book: Book, actions: BookActions, onMore: () -> Unit) {
        activity.lifecycleScope.launch {
            val category = book.categoryId?.let { id -> db.categoryDao().getAllCategories().find { it.id == id } }
            val series = book.seriesId?.let { db.seriesDao().getById(it) }
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.sheet_book_detail, null)
            val dialog = BottomSheetDialog(activity)
            val density = activity.resources.displayMetrics.density

            fun themeColor(attr: Int): Int {
                val tv = TypedValue()
                activity.theme.resolveAttribute(attr, tv, true)
                return tv.data
            }

            // ---- header ----
            view.findViewById<TextView>(R.id.detailTitle).text = book.title
            val subtitle = listOfNotNull(
                book.author?.takeIf { it.isNotBlank() },
                book.year?.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            view.findViewById<TextView>(R.id.detailSubtitle).apply {
                text = subtitle
                visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE
            }

            val chips = view.findViewById<LinearLayout>(R.id.detailChips)
            fun addChip(text: String) {
                val chip = TextView(activity)
                chip.text = text
                chip.textSize = 12f
                chip.setTextColor(themeColor(R.attr.appColorPrimary))
                chip.setBackgroundResource(R.drawable.badge_outline_bg)
                val h = (10 * density).toInt()
                val v = (3 * density).toInt()
                chip.setPadding(h, v, h, v)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = (8 * density).toInt()
                chip.layoutParams = lp
                chips.addView(chip)
            }
            if (book.isFavorite) addChip("★ Favourite")
            if (book.isDraft) addChip("Draft")
            if (book.copies > 1) addChip("×${book.copies} copies")
            if (book.flaggedDuplicate) addChip("Possible duplicate")
            if (book.lentTo != null) addChip("Lent")
            if (chips.childCount == 0) chips.visibility = View.GONE

            // ---- info rows ----
            val info = view.findViewById<LinearLayout>(R.id.infoCard)
            var rowCount = 0
            fun addRow(label: String, value: String?) {
                if (value.isNullOrBlank()) return
                val row = inflater.inflate(R.layout.item_detail_row, info, false)
                row.findViewById<TextView>(R.id.detailLabel).text = label
                row.findViewById<TextView>(R.id.detailValue).text = value
                row.findViewById<View>(R.id.detailDivider).visibility = if (rowCount == 0) View.GONE else View.VISIBLE
                info.addView(row)
                rowCount++
            }
            addRow("Category", category?.name)
            addRow(
                "Series",
                series?.let { s ->
                    s.name + (book.seriesNumber?.let { n -> " · #$n" + (s.totalVolumes?.let { t -> " of $t" } ?: "") } ?: "")
                }
            )
            addRow("ISBN", book.isbn)
            addRow("Copies", book.copies.toString())
            if (rowCount == 0) info.visibility = View.GONE

            // ---- lending ----
            if (book.lentTo != null) {
                val now = System.currentTimeMillis()
                val overdue = book.dueAt != null && book.dueAt < now
                val lines = mutableListOf("Lent to ${book.lentTo}")
                book.lentAt?.let { lines.add("Since ${DateFormat.format("MMM d, yyyy", it)}") }
                book.dueAt?.let { lines.add("Due ${DateFormat.format("MMM d, yyyy", it)}" + if (overdue) " — overdue" else "") }
                view.findViewById<LinearLayout>(R.id.lendCard).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.lendText).apply {
                    text = lines.joinToString("\n")
                    if (overdue) setTextColor(themeColor(R.attr.appColorDelete))
                }
            }

            // ---- draft: where it was started + what's missing ----
            if (book.isDraft) {
                val origin = when {
                    series != null ->
                        "You were adding this to the series \u201C${series.name}\u201D" +
                                (book.seriesNumber?.let { " as volume #$it" } ?: "") + "."
                    category != null -> "You were adding this to the category \u201C${category.name}\u201D."
                    else -> "You were adding this from the main list (no category or series)."
                }
                val needed = mutableListOf<String>()
                if (book.title == BookDialogHelper.UNTITLED_TITLE) needed.add("A title")
                if (book.seriesId != null && book.seriesNumber == null) needed.add("A volume number (required for a series)")
                if (book.flaggedDuplicate) needed.add("Check the possible duplicate")
                view.findViewById<LinearLayout>(R.id.draftCard).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.draftOriginText).text = origin
                view.findViewById<TextView>(R.id.draftNeededText).text =
                    if (needed.isEmpty()) "Nothing — it's ready to publish." else needed.joinToString("\n") { "• $it" }
            }

            // ---- buttons ----
            view.findViewById<Button>(R.id.primaryButton).apply {
                text = if (book.isDraft) "Continue editing" else "Edit"
                setOnClickListener {
                    dialog.dismiss()
                    actions.edit(book).onClick()
                }
            }
            view.findViewById<Button>(R.id.optionsButton).setOnClickListener {
                dialog.dismiss()
                onMore()
            }
            if (book.isDraft) {
                view.findViewById<Button>(R.id.publishButton).apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        dialog.dismiss()
                        actions.publish(book).onClick()
                    }
                }
            }

            dialog.setContentView(view)
            (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.show()
        }
    }
}