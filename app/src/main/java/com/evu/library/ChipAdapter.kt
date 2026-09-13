package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChipAdapter(
    private var chips: List<ChipItem>,
    private var selectedIndex: Int,
    private val onSelect: (Int, ChipItem) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

    class ChipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.chipText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chip = chips[position]
        holder.text.text = when (chip) {
            is ChipItem.All -> "All"
            is ChipItem.Favourites -> "Favorites"
            is ChipItem.CategoryChip -> chip.category.name
            is ChipItem.AddNew -> "+"
        }
        holder.text.isSelected = position == selectedIndex
        holder.itemView.setOnClickListener {
            if (chip is ChipItem.AddNew) {
                onSelect(position, chip)
            } else {
                val old = selectedIndex
                selectedIndex = position
                notifyItemChanged(old)
                notifyItemChanged(position)
                onSelect(position, chip)
            }
        }
    }

    override fun getItemCount() = chips.size

    fun updateChips(newChips: List<ChipItem>, newSelectedIndex: Int) {
        chips = newChips
        selectedIndex = newSelectedIndex
        notifyDataSetChanged()
    }
}