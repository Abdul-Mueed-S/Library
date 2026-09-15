package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ThemeAdapter(
    private val themeKeys: List<String>,
    private var selectedKey: String,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    class ThemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRow: View = view.findViewById(R.id.themeCardRow)
        val swatch: View = view.findViewById(R.id.themeSwatch)
        val name: TextView = view.findViewById(R.id.themeNameText)
        val check: TextView = view.findViewById(R.id.themeCheckText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val key = themeKeys[position]
        holder.name.text = AppPrefs.getThemeDisplayName(key)
        holder.check.visibility = if (key == selectedKey) View.VISIBLE else View.GONE

        val swatchColor = AppPrefs.getThemeSwatchColor(key)
        val drawable = holder.swatch.background.mutate()
        (drawable as? android.graphics.drawable.GradientDrawable)?.setColor(swatchColor)

        holder.cardRow.setOnClickListener {
            val oldSelected = selectedKey
            selectedKey = key
            notifyItemChanged(themeKeys.indexOf(oldSelected))
            notifyItemChanged(position)
            onSelect(key)
        }
    }

    override fun getItemCount() = themeKeys.size
}