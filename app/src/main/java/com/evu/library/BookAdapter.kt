package com.evu.library

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
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

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: ViewGroup = view as ViewGroup
        val numberText: TextView = view.findViewById(R.id.numberText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val expandButton: TextView = view.findViewById(R.id.expandButton)
        val optionsButton: TextView = view.findViewById(R.id.optionsButton)
        val detailsContainer: LinearLayout = view.findViewById(R.id.detailsContainer)
        val subtitleText: TextView = view.findViewById(R.id.subtitleText)
        val isbnText: TextView = view.findViewById(R.id.isbnText)
        val duplicateChipText: TextView = view.findViewById(R.id.duplicateChipText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        val context: Context = holder.itemView.context

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

        holder.itemView.setOnLongClickListener {
            onOptions(book)
            true
        }
        holder.optionsButton.setOnClickListener {
            onOptions(book)
        }
    }

    override fun getItemCount() = books.size

    fun updateList(newList: List<Book>) {
        books = newList
        notifyDataSetChanged()
    }
}