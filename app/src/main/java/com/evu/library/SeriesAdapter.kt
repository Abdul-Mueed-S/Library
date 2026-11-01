package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SeriesRow(val series: Series, val count: Int, val owned: Set<Int>) {
    private val upper: Int get() = maxOf(series.totalVolumes ?: 0, owned.maxOrNull() ?: 0)
    val missing: List<Int> get() = (1..upper).filter { it !in owned }

    val summary: String
        get() = when {
            series.totalVolumes != null && owned.isNotEmpty() && missing.isEmpty() ->
                "Complete — all ${series.totalVolumes} volumes"
            series.totalVolumes != null -> "Own ${owned.size} of ${series.totalVolumes}"
            else -> "$count ${if (count == 1) "entry" else "entries"}"
        }

    val missingText: String? get() =
        if (missing.isEmpty()) null else "Missing: " + missing.joinToString(", ") { "#$it" }
}

class SeriesAdapter(
    private var items: List<SeriesRow>,
    private val onClick: (SeriesRow) -> Unit,
    private val onLongClick: (SeriesRow) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val row: View = view.findViewById(R.id.seriesCardRow)
        val name: TextView = view.findViewById(R.id.seriesNameText)
        val summary: TextView = view.findViewById(R.id.seriesSummaryText)
        val missing: TextView = view.findViewById(R.id.seriesMissingText)
        val badge: TextView = view.findViewById(R.id.seriesCountBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_series, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.series.name
        holder.summary.text = item.summary
        val missingText = item.missingText
        holder.missing.text = missingText ?: ""
        holder.missing.visibility = if (missingText != null) View.VISIBLE else View.GONE
        holder.badge.text = item.count.toString()
        holder.row.setOnClickListener { onClick(item) }
        holder.row.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SeriesRow>) {
        items = newItems
        notifyDataSetChanged()
    }
}