package com.evu.library

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SnapshotAdapter(
    private var items: List<SnapshotInfo>,
    private val onClick: (SnapshotInfo) -> Unit
) : RecyclerView.Adapter<SnapshotAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val row: View = view.findViewById(R.id.snapshotCardRow)
        val name: TextView = view.findViewById(R.id.snapshotNameText)
        val subtitle: TextView = view.findViewById(R.id.snapshotSubtitleText)
        val whereBadge: TextView = view.findViewById(R.id.snapshotWhereBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_snapshot, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val info = items[position]
        holder.name.text = info.name

        val date = DateFormat.format("MMM d, yyyy  •  h:mm a", info.createdAt)
        holder.subtitle.text =
            if (info.libraryCount >= 0) "$date  •  ${info.libraryCount} libraries, ${info.bookCount} books" else date

        holder.whereBadge.text = when {
            info.onPhone && info.inFolder -> "Phone + Folder"
            info.onPhone -> "Phone only"
            else -> "Folder only"
        }
        holder.row.setOnClickListener { onClick(info) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<SnapshotInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}