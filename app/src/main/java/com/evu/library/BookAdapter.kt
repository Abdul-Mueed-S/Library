package com.evu.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookAdapter(
    private var books: List<Book>,
    private val onOptions: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberText: TextView = view.findViewById(R.id.numberText)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val optionsButton: TextView = view.findViewById(R.id.optionsButton)
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

        val titlePrefix = buildString {
            if (book.isFavorite) append("★ ")
            if (book.isDraft) append("📝 ")
        }
        holder.titleText.text = "$titlePrefix${book.title}"

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

        holder.duplicateChipText.visibility = if (book.flaggedDuplicate) View.VISIBLE else View.GONE

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