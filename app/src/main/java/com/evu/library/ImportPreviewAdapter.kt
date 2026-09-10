package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ImportPreviewAdapter(
    private var items: MutableList<ImportedBook>,
    private val includeCategoriesProvider: () -> Boolean,
    private val onEdit: (Int) -> Unit,
    private val onRemove: (Int) -> Unit,
    private val onDuplicateTagClick: (ImportedBook) -> Unit
) : RecyclerView.Adapter<ImportPreviewAdapter.ImportViewHolder>() {

    class ImportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberText: TextView = view.findViewById(R.id.importNumberText)
        val title: TextView = view.findViewById(R.id.importTitleText)
        val duplicateTag: TextView = view.findViewById(R.id.duplicateTag)
        val details: TextView = view.findViewById(R.id.importDetailsText)
        val editBtn: TextView = view.findViewById(R.id.editImportButton)
        val removeBtn: TextView = view.findViewById(R.id.removeImportButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_import_book, parent, false)
        return ImportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImportViewHolder, position: Int) {
        val item = items[position]

        val showNumbers = AppPrefs.isShowNumbers(holder.itemView.context)
        holder.numberText.visibility = if (showNumbers) View.VISIBLE else View.GONE
        holder.numberText.text = "${position + 1}."

        holder.title.text = item.title
        holder.duplicateTag.visibility = if (item.isDuplicate) View.VISIBLE else View.GONE
        holder.duplicateTag.setOnClickListener { onDuplicateTagClick(item) }

        val includeCategories = includeCategoriesProvider()
        val detailsParts = mutableListOf<String>()
        item.author?.let { detailsParts.add("Author: $it") }
        item.isbn?.let { detailsParts.add("ISBN: $it") }
        if (includeCategories) item.categoryName?.let { detailsParts.add("Category: $it") }
        holder.details.text = detailsParts.joinToString(" • ")

        holder.editBtn.setOnClickListener { onEdit(position) }
        holder.removeBtn.setOnClickListener { onRemove(position) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: MutableList<ImportedBook>) {
        items = newList
        notifyDataSetChanged()
    }
}