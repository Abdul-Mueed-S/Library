package com.evu.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val destUriString = BackupPrefs.getDestinationUri(context)
        if (destUriString == null || !BackupPrefs.isEnabled(context)) {
            return Result.success()
        }

        val destUri = Uri.parse(destUriString)
        val destFolder = DocumentFile.fromTreeUri(context, destUri)
        if (destFolder == null || !destFolder.canWrite()) {
            BackupPrefs.recordBackupResult(context, success = false)
            return Result.failure()
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        var allSucceeded = true

        // Vault registry (names + locations) — one file per run, covers all vaults at once.
        try {
            val metadataJson = VaultManager.exportVaultMetadataJson(context)
            val metaFile = destFolder.createFile("application/json", "library_vaults_metadata_$timestamp.json")
            if (metaFile != null) {
                context.contentResolver.openOutputStream(metaFile.uri)?.use { it.write(metadataJson.toByteArray()) }
                    ?: run { allSucceeded = false }
            } else {
                allSucceeded = false
            }
        } catch (e: Exception) {
            allSucceeded = false
        }

        // Per-vault book/category data.
        val vaults = VaultManager.getAllVaults(context)
        for (vault in vaults) {
            try {
                val db = AppDatabase.getDatabaseForVault(context, vault)
                val books = db.bookDao().getAllBooks()
                val categories = db.categoryDao().getAllCategories()
                val json = ExportImportUtils.fullBackupToJson(categories, books)

                val safeVaultName = vault.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val fileName = "library_backup_${safeVaultName}_$timestamp.json"

                val newFile = destFolder.createFile("application/json", fileName)
                if (newFile == null) {
                    allSucceeded = false
                    continue
                }
                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    out.write(json.toByteArray())
                } ?: run { allSucceeded = false }
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        BackupPrefs.recordBackupResult(context, success = allSucceeded)
        return if (allSucceeded) Result.success() else Result.retry()
    }
}