package com.evu.library

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
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

    // Reports the current number of selected books (0 = selection mode ended).
    var onSelectionChanged: ((Int) -> Unit)? = null

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: ViewGroup = view as ViewGroup
        val card: View = view.findViewById(R.id.bookCard)
        val numberText: TextView = view.findViewById(R.id.numberText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val expandButton: TextView = view.findViewById(R.id.expandButton)
        val selectIndicator: TextView = view.findViewById(R.id.selectIndicator)
        val optionsButton: TextView = view.findViewById(R.id.optionsButton)
        val detailsContainer: LinearLayout = view.findViewById(R.id.detailsContainer)
        val subtitleText: TextView = view.findViewById(R.id.subtitleText)
        val isbnText: TextView = view.findViewById(R.id.isbnText)
        val duplicateChipText: TextView = view.findViewById(R.id.duplicateChipText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        val context: Context = holder.itemView.context
        val selecting = selectedIds.isNotEmpty()
        val isSelected = selectedIds.contains(book.id)

        val showNumbers = AppPrefs.isShowNumbers(context)
        holder.numberText.visibility = if (showNumbers) View.VISIBLE else View.GONE
        holder.numberText.text = "${position + 1}."

        val titlePrefix = if (book.isFavorite) "★ " else ""
        val titleSuffix = if (book.isDraft) " (Draft)" else ""
        holder.titleText.text = "$titlePrefix${book.title}$titleSuffix"

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

        // Selection visuals: highlighted border, ⋮ replaced by a check / empty circle.
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
            notifyDataSetChanged() // entering/leaving selection mode changes every row
        } else {
            val index = books.indexOfFirst { it.id == book.id }
            if (index >= 0) notifyItemChanged(index)
        }
        onSelectionChanged?.invoke(selectedIds.size)
    }
}