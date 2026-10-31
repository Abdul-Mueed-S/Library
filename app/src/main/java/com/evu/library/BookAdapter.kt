package com.evu.library

import android.content.Context
import android.text.format.DateFormat
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookAdapter(
    private var books: List<Book>,
    private val showDuplicateTag: Boolean = true,
    private val onOptions: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val expandedIds = mutableSetOf<Int>()
    private val selectedIds = mutableSetOf<Int>()

    var onSelectionChanged: ((Int) -> Unit)? = null

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: ViewGroup = view as ViewGroup
        val card: View = view.findViewById(R.id.bookCard)
        val numberText: TextView = view.findViewById(R.id.numberText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val copiesBadge: TextView = view.findViewById(R.id.copiesBadge)
        val lentBadge: TextView = view.findViewById(R.id.lentBadge)
        val expandButton: TextView = view.findViewById(R.id.expandButton)
        val selectIndicator: TextView = view.findViewById(R.id.selectIndicator)
        val optionsButton: TextView = view.findViewById(R.id.optionsButton)
        val detailsContainer: LinearLayout = view.findViewById(R.id.detailsContainer)
        val subtitleText: TextView = view.findViewById(R.id.subtitleText)
        val seriesText: TextView = view.findViewById(R.id.seriesText)
        val isbnText: TextView = view.findViewById(R.id.isbnText)
        val lentText: TextView = view.findViewById(R.id.lentText)
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

    private fun fmt(millis: Long): String = DateFormat.format("MMM d, yyyy", millis).toString()

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        val context: Context = holder.itemView.context
        val selecting = selectedIds.isNotEmpty()
        val isSelected = selectedIds.contains(book.id)

        holder.numberText.visibility = if (AppPrefs.isShowNumbers(context)) View.VISIBLE else View.GONE
        holder.numberText.text = "${position + 1}."

        val titlePrefix = if (book.isFavorite) "★ " else ""
        val titleSuffix = if (book.isDraft) " (Draft)" else ""
        holder.titleText.text = "$titlePrefix${book.title}$titleSuffix"

        // Copies badge — only when you own more than one.
        holder.copiesBadge.visibility = if (book.copies > 1) View.VISIBLE else View.GONE
        holder.copiesBadge.text = "×${book.copies}"

        // Lending badge + line.
        if (book.lentTo != null) {
            val overdue = book.dueAt != null && book.dueAt < System.currentTimeMillis()
            holder.lentBadge.text = if (overdue) "Overdue" else "Lent"
            holder.lentBadge.setBackgroundResource(if (overdue) R.drawable.badge_delete_bg else R.drawable.badge_outline_bg)
            holder.lentBadge.setTextColor(attrColor(context, if (overdue) R.attr.appColorOnPrimary else R.attr.appColorPrimary))
            holder.lentBadge.visibility = View.VISIBLE
            holder.lentText.text = buildString {
                append("Lent to ${book.lentTo}")
                book.lentAt?.let { append(" · since ${fmt(it)}") }
                book.dueAt?.let { append(" · due ${fmt(it)}") }
                if (overdue) append(" (overdue)")
            }
            holder.lentText.visibility = View.VISIBLE
        } else {
            holder.lentBadge.visibility = View.GONE
            holder.lentText.visibility = View.GONE
        }

        val subtitleParts = mutableListOf<String>()
        book.author?.takeIf { it.isNotBlank() }?.let { subtitleParts.add(it) }
        book.edition?.takeIf { it.isNotBlank() }?.let { subtitleParts.add("Ed. $it") }
        book.year?.takeIf { it.isNotBlank() }?.let { subtitleParts.add(it) }
        if (subtitleParts.isNotEmpty()) {
            holder.subtitleText.text = subtitleParts.joinToString("  •  ")
            holder.subtitleText.visibility = View.VISIBLE
        } else {
            holder.subtitleText.visibility = View.GONE
        }

        if (!book.seriesName.isNullOrBlank()) {
            holder.seriesText.text = buildString {
                append("Series: ${book.seriesName}")
                book.seriesNumber?.let {
                    append(" · #$it")
                    book.seriesTotal?.let { total -> append(" of $total") }
                }
            }
            holder.seriesText.visibility = View.VISIBLE
        } else {
            holder.seriesText.visibility = View.GONE
        }

        if (!book.isbn.isNullOrBlank()) {
            holder.isbnText.text = "ISBN ${book.isbn}"
            holder.isbnText.visibility = View.VISIBLE
        } else {
            holder.isbnText.visibility = View.GONE
        }

        holder.duplicateChipText.visibility =
            if (showDuplicateTag && book.flaggedDuplicate) View.VISIBLE else View.GONE

        val isExpanded = expandedIds.contains(book.id)
        holder.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.expandButton.text = if (isExpanded) "▴" else "▾"

        holder.expandButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(holder.root, AutoTransition().apply { duration = 180 })
            if (expandedIds.contains(book.id)) {
                expandedIds.remove(book.id)
                holder.detailsContainer.visibility = View.GONE
                holder.expandButton.text = "▾"
            } else {
                expandedIds.add(book.id)
                holder.detailsContainer.visibility = View.VISIBLE
                holder.expandButton.text = "▴"
            }
        }

        holder.card.isActivated = isSelected
        holder.optionsButton.visibility = if (selecting) View.GONE else View.VISIBLE
        holder.selectIndicator.visibility = if (selecting) View.VISIBLE else View.GONE
        holder.selectIndicator.text = if (isSelected) "✓" else "○"

        holder.itemView.setOnClickListener {
            if (selectedIds.isNotEmpty()) toggleSelection(book)
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