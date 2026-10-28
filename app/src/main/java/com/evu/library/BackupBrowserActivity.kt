package com.evu.library

import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupBrowserActivity : BaseActivity() {

    private lateinit var adapter: BackupFileAdapter
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_browser)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        statusText = findViewById(R.id.statusText)

        adapter = BackupFileAdapter(emptyList()) { entry -> confirmRestore(entry) }
        val recycler = findViewById<RecyclerView>(R.id.backupsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadBackups()
    }

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

    private fun loadBackups() {
        val uriString = BackupPrefs.getDestinationUri(this)
        if (uriString == null) {
            showStatus("No backup folder chosen yet. Choose one in Settings.")
            return
        }
        showStatus("Loading backups…")
        lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) {
                try {
                    val folder = DocumentFile.fromTreeUri(this@BackupBrowserActivity, Uri.parse(uriString))
                    if (folder != null && folder.canRead()) BackupFiles.listBackups(folder) else null
                } catch (e: Exception) {
                    null
                }
            }
            when {
                entries == null -> showStatus("Couldn't read the backup folder. Re-choose it in Settings.")
                entries.isEmpty() -> showStatus("No backups found in this folder yet.")
                else -> {
                    statusText.visibility = View.GONE
                    adapter.updateRows(buildRows(entries))
                }
            }
        }
    }

    private fun buildRows(entries: List<BackupEntry>): List<BackupRow> {
        val activeKey = BackupFiles.DATA_PREFIX + BackupFiles.safeVaultName(VaultManager.getActiveVault(this).name)
        val dataGroups = entries.filter { it.kind == BackupKind.LIBRARY_DATA }.groupBy { it.groupKey }
        val metadataEntries = entries.filter { it.kind == BackupKind.VAULT_METADATA }

        val orderedKeys = dataGroups.keys.sortedWith(compareByDescending<String> { it == activeKey }.thenBy { it })
        val rows = mutableListOf<BackupRow>()
        for (key in orderedKeys) {
            val group = dataGroups.getValue(key)
            val suffix = if (key == activeKey) "  (current library)" else ""
            rows.add(BackupRow.Header(group.first().groupLabel + suffix))
            group.forEach { rows.add(BackupRow.Item(it)) }
        }
        if (metadataEntries.isNotEmpty()) {
            rows.add(BackupRow.Header(BackupFiles.METADATA_LABEL))
            metadataEntries.forEach { rows.add(BackupRow.Item(it)) }
        }
        return rows
    }

    private fun confirmRestore(entry: BackupEntry) {
        val whenText = entry.timestamp?.let { DateFormat.format("MMM d, yyyy h:mm a", it).toString() }
            ?: (entry.file.name ?: "this backup")

        if (entry.kind == BackupKind.VAULT_METADATA) {
            AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setTitle("Restore Names & Locations")
                .setMessage("Creates any missing libraries and applies the saved names and locations from $whenText. Books are not touched.")
                .setPositiveButton("Restore") { _, _ -> restoreMetadata(entry) }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val active = VaultManager.getActiveVault(this)
        val activeKey = BackupFiles.DATA_PREFIX + BackupFiles.safeVaultName(active.name)
        val mismatch = if (entry.groupKey != activeKey)
            "\n\nNote: this backup is from \"${entry.groupLabel}\" but will replace the CURRENT library." else ""

        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Restore Backup")
            .setMessage(
                "This will DELETE everything in \"${active.name}\" and replace it with the backup from $whenText. " +
                        "This cannot be undone.$mismatch"
            )
            .setPositiveButton("Restore") { _, _ -> restoreLibrary(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun readText(entry: BackupEntry): String? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(entry.file.uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    private fun restoreLibrary(entry: BackupEntry) {
        lifecycleScope.launch {
            val data = readText(entry)?.let { ExportImportUtils.parseFullBackupJson(it) }
            if (data == null) {
                Toast.makeText(this@BackupBrowserActivity, "Invalid or unreadable backup file", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                BackupRestorer.restoreIntoDatabase(AppDatabase.getDatabase(this@BackupBrowserActivity), data)
                Toast.makeText(
                    this@BackupBrowserActivity,
                    "Restored ${data.books.size} books, ${data.categories.size} categories",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@BackupBrowserActivity, "Restore failed — your library was not changed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreMetadata(entry: BackupEntry) {
        lifecycleScope.launch {
            val text = readText(entry)
            if (text == null) {
                Toast.makeText(this@BackupBrowserActivity, "Couldn't read the file", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val count = VaultManager.restoreVaultMetadataJson(this@BackupBrowserActivity, text)
                Toast.makeText(
                    this@BackupBrowserActivity,
                    "Restored $count librar${if (count == 1) "y" else "ies"} (names/locations only)",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@BackupBrowserActivity, "Invalid vault metadata file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}