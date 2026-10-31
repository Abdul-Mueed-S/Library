package com.evu.library

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SnapshotsActivity : BaseActivity() {

    private lateinit var adapter: SnapshotAdapter
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snapshots)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        statusText = findViewById(R.id.statusText)
        findViewById<TextView>(R.id.introText).text =
            "Point-in-time copies of ALL your libraries (books, categories, names and locations), " +
                    "created only when you ask. Each is kept on this phone and also copied into your backup folder " +
                    "when one is set. Restoring replaces the books and categories of every library in the snapshot; " +
                    "libraries created after it are left as they are."

        adapter = SnapshotAdapter(emptyList()) { info -> showOptions(info) }
        val recycler = findViewById<RecyclerView>(R.id.snapshotsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.createSnapshotButton).setOnClickListener { promptCreate() }
        refresh()
    }

    private fun refresh() {
        statusText.text = "Loading…"
        statusText.visibility = View.VISIBLE
        lifecycleScope.launch {
            val items = SnapshotManager.listAll(this@SnapshotsActivity)
            adapter.update(items)
            statusText.text = "No snapshots yet. Create one before a big clean-up."
            statusText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun promptCreate() {
        val input = EditText(this)
        input.hint = "Name (optional), e.g. Before spring clean"
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Create Snapshot")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                lifecycleScope.launch {
                    val result = SnapshotManager.create(this@SnapshotsActivity, input.text.toString())
                    refresh()
                    val message = when {
                        !result.folderConfigured -> "Snapshot saved on this phone. Choose a backup folder in Settings to also keep a copy there."
                        result.savedToFolder -> "Snapshot saved on this phone and in your backup folder"
                        else -> "Saved on this phone, but copying to the backup folder failed"
                    }
                    Toast.makeText(this@SnapshotsActivity, message, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOptions(info: SnapshotInfo) {
        OptionsDialogHelper.show(
            this, info.name,
            listOf(
                DialogOption(android.R.drawable.ic_menu_revert, "Restore this snapshot"),
                DialogOption(android.R.drawable.ic_menu_delete, "Delete snapshot")
            )
        ) { which ->
            if (which == 0) confirmRestore(info) else confirmDelete(info)
        }
    }

    private fun confirmRestore(info: SnapshotInfo) {
        val whenText = DateFormat.format("MMM d, yyyy h:mm a", info.createdAt)
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Restore Snapshot")
            .setMessage(
                "Restore \"${info.name}\" from $whenText?\n\n" +
                        "Every library in the snapshot has its books and categories REPLACED with the saved versions. " +
                        "Libraries created after this snapshot are left as they are. This cannot be undone — " +
                        "choose \"Snapshot first, then restore\" to keep the current state."
            )
            .setPositiveButton("Restore") { _, _ -> doRestore(info, snapshotFirst = false) }
            .setNeutralButton("Snapshot first, then restore") { _, _ -> doRestore(info, snapshotFirst = true) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doRestore(info: SnapshotInfo, snapshotFirst: Boolean) {
        lifecycleScope.launch {
            if (snapshotFirst) {
                SnapshotManager.create(this@SnapshotsActivity, "Before restoring \"${info.name}\"")
            }
            val result = try {
                SnapshotManager.restore(this@SnapshotsActivity, info)
            } catch (e: Exception) {
                null
            }
            refresh()
            val message = when {
                result == null -> "Couldn't read this snapshot — nothing was changed"
                result.restored == result.total -> "Restored ${result.total} librar${if (result.total == 1) "y" else "ies"}"
                else -> "Restored ${result.restored} of ${result.total} libraries"
            }
            Toast.makeText(this@SnapshotsActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDelete(info: SnapshotInfo) {
        val where = if (info.onPhone && info.inFolder) "this phone and your backup folder"
        else if (info.inFolder) "your backup folder" else "this phone"
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Delete Snapshot")
            .setMessage("Delete \"${info.name}\" from $where? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    SnapshotManager.delete(this@SnapshotsActivity, info)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}