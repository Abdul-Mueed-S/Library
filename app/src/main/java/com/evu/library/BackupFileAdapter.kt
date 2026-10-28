package com.evu.library

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class BackupRow {
    data class Header(val label: String) : BackupRow()
    data class Item(val entry: BackupEntry) : BackupRow()
}

class BackupFileAdapter(
    private var rows: List<BackupRow>,
    private val onClick: (BackupEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.backupHeaderText)
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val row: View = view.findViewById(R.id.backupCardRow)
        val title: TextView = view.findViewById(R.id.backupTitleText)
        val subtitle: TextView = view.findViewById(R.id.backupSubtitleText)
    }

    override fun getItemViewType(position: Int) =
        if (rows[position] is BackupRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_backup_header, parent, false))
        } else {
            ItemHolder(inflater.inflate(R.layout.item_backup_file, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is BackupRow.Header -> (holder as HeaderHolder).label.text = row.label.uppercase()
            is BackupRow.Item -> {
                val itemHolder = holder as ItemHolder
                val timestamp = row.entry.timestamp
                itemHolder.title.text =
                    if (timestamp != null) DateFormat.format("MMM d, yyyy  •  h:mm a", timestamp)
                    else row.entry.file.name ?: "Backup"
                itemHolder.subtitle.text = row.entry.file.name ?: ""
                itemHolder.row.setOnClickListener { onClick(row.entry) }
            }
        }
    }

    override fun getItemCount() = rows.size

    fun updateRows(newRows: List<BackupRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}