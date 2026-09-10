package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<Category>,
    private val onClick: (Category) -> Unit,
    private val onLongClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberText: TextView = view.findViewById(R.id.categoryNumberText)
        val nameText: TextView = view.findViewById(R.id.categoryNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val showNumbers = AppPrefs.isShowNumbers(holder.itemView.context)
        holder.numberText.visibility = if (showNumbers) View.VISIBLE else View.GONE
        holder.numberText.text = "${position + 1}."

        holder.nameText.text = category.name
        holder.itemView.setOnClickListener { onClick(category) }
        holder.itemView.setOnLongClickListener {
            onLongClick(category)
            true
        }
    }

    override fun getItemCount() = categories.size

    fun updateList(newList: List<Category>) {
        categories = newList
        notifyDataSetChanged()
    }
}