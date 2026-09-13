package com.evu.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
        val detailsLayout: LinearLayout = view.findViewById(R.id.detailsLayout)
        val authorText: TextView = view.findViewById(R.id.authorText)
        val editionText: TextView = view.findViewById(R.id.editionText)
        val isbnText: TextView = view.findViewById(R.id.isbnText)
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

        val hasAuthor = !book.author.isNullOrBlank()
        val hasEditionYear = !book.edition.isNullOrBlank() || !book.year.isNullOrBlank()
        val hasIsbn = !book.isbn.isNullOrBlank()

        holder.authorText.visibility = if (hasAuthor) View.VISIBLE else View.GONE
        holder.authorText.text = "Author: ${book.author}"

        holder.editionText.visibility = if (hasEditionYear) View.VISIBLE else View.GONE
        val editionYearText = buildString {
            if (!book.edition.isNullOrBlank()) append("Edition: ${book.edition}")
            if (!book.edition.isNullOrBlank() && !book.year.isNullOrBlank()) append("\n")
            if (!book.year.isNullOrBlank()) append("Year: ${book.year}")
        }
        holder.editionText.text = editionYearText

        holder.isbnText.visibility = if (hasIsbn) View.VISIBLE else View.GONE
        holder.isbnText.text = "ISBN: ${book.isbn}"

        holder.detailsLayout.visibility = View.GONE
        holder.titleText.setOnClickListener {
            holder.detailsLayout.visibility =
                if (holder.detailsLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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