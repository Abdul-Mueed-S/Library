package com.evu.library

import android.content.Context
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookAdapter(
    private var books: List<Book>,
    private val showDuplicateTag: Boolean = true,
    private val onOptions: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val selectedIds = mutableSetOf<Int>()

    var seriesById: Map<Int, Series> = emptyMap()
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onOpen: ((Book) -> Unit)? = null

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.bookCard)
        val numberText: TextView = view.findViewById(R.id.numberText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val copiesBadge: TextView = view.findViewById(R.id.copiesBadge)
        val lentBadge: TextView = view.findViewById(R.id.lentBadge)
        val selectIndicator: TextView = view.findViewById(R.id.selectIndicator)
        val optionsButton: TextView = view.findViewById(R.id.optionsButton)
        val subtitleText: TextView = view.findViewById(R.id.subtitleText)
        val seriesText: TextView = view.findViewById(R.id.seriesText)
        val duplicateChipText: TextView = view.findViewById(R.id.duplicateChipText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    private fun attrColor(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        val context: Context = holder.itemView.context
        val selecting = selectedIds.isNotEmpty()
        val isSelected = selectedIds.contains(book.id)

        val pad = ((if (AppPrefs.isCompact(context)) 8 else 14) * context.resources.displayMetrics.density).toInt()
        holder.card.setPadding(pad, pad, pad, pad)

        holder.numberText.visibility = if (AppPrefs.isShowNumbers(context)) View.VISIBLE else View.GONE
        holder.numberText.text = "${position + 1}."

        val titlePrefix = if (book.isFavorite) "★ " else ""
        val titleSuffix = if (book.isDraft) " (Draft)" else ""
        holder.titleText.text = "$titlePrefix${book.title}$titleSuffix"

        holder.copiesBadge.visibility = if (book.copies > 1) View.VISIBLE else View.GONE
        holder.copiesBadge.text = "×${book.copies}"

        if (book.lentTo != null) {
            val overdue = book.dueAt != null && book.dueAt < System.currentTimeMillis()
            holder.lentBadge.text = if (overdue) "Overdue" else "Lent"
            holder.lentBadge.setBackgroundResource(if (overdue) R.drawable.badge_delete_bg else R.drawable.badge_outline_bg)
            holder.lentBadge.setTextColor(attrColor(context, if (overdue) R.attr.appColorOnPrimary else R.attr.appColorPrimary))
            holder.lentBadge.visibility = View.VISIBLE
        } else {
            holder.lentBadge.visibility = View.GONE
        }

        val subtitle = listOfNotNull(
            book.author?.takeIf { it.isNotBlank() },
            book.year?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        holder.subtitleText.text = subtitle
        holder.subtitleText.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE

        val series = book.seriesId?.let { seriesById[it] }
        if (series != null) {
            holder.seriesText.text = series.name + (book.seriesNumber?.let { " · #$it" } ?: "")
            holder.seriesText.visibility = View.VISIBLE
        } else {
            holder.seriesText.visibility = View.GONE
        }

        holder.duplicateChipText.visibility =
            if (showDuplicateTag && book.flaggedDuplicate) View.VISIBLE else View.GONE

        holder.card.isActivated = isSelected
        holder.optionsButton.visibility = if (selecting) View.GONE else View.VISIBLE
        holder.selectIndicator.visibility = if (selecting) View.VISIBLE else View.GONE
        holder.selectIndicator.text = if (isSelected) "✓" else "○"

        holder.itemView.setOnClickListener {
            if (selectedIds.isNotEmpty()) toggleSelection(book) else onOpen?.invoke(book)
        }
        holder.itemView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            toggleSelection(book)
            true
        }
        holder.optionsButton.setOnClickListener { onOptions(book) }
    }

    override fun getItemCount() = books.size

    fun updateList(newList: List<Book>) {
        books = newList
        val before = selectedIds.size
        selectedIds.retainAll(newList.map { it.id }.toSet())
        notifyDataSetChanged()
        if (selectedIds.size != before) onSelectionChanged?.invoke(selectedIds.size)
    }

    fun getSelectedBooks(): List<Book> = books.filter { selectedIds.contains(it.id) }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(0)
    }

    fun selectAll() {
        selectedIds.addAll(books.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size)
    }

    private fun toggleSelection(book: Book) {
        val wasSelecting = selectedIds.isNotEmpty()
        if (!selectedIds.remove(book.id)) selectedIds.add(book.id)
        val isSelecting = selectedIds.isNotEmpty()
        if (wasSelecting != isSelecting) {
            notifyDataSetChanged()
        } else {
            val index = books.indexOfFirst { it.id == book.id }
            if (index >= 0) notifyItemChanged(index)
        }
        onSelectionChanged?.invoke(selectedIds.size)
    }
}