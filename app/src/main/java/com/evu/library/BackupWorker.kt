package com.evu.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val isManual = inputData.getBoolean(KEY_MANUAL, false)
        val destUriString = BackupPrefs.getDestinationUri(context)
        if (destUriString == null || (!isManual && !BackupPrefs.isEnabled(context))) {
            return@withContext Result.success()
        }

        val destFolder = DocumentFile.fromTreeUri(context, Uri.parse(destUriString))
        if (destFolder == null || !destFolder.canWrite()) {
            BackupPrefs.recordBackupResult(context, success = false)
            return@withContext Result.failure()
        }

        val timestamp = BackupFiles.newTimestamp()
        var allSucceeded = true

        // Vault registry (names + locations) — one file per run.
        try {
            val metadataJson = VaultManager.exportVaultMetadataJson(context)
            val metaFile = destFolder.createFile("application/json", BackupFiles.metadataFileName(timestamp))
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
        for (vault in VaultManager.getAllVaults(context)) {
            try {
                val db = AppDatabase.getDatabaseForVault(context, vault)
                val json = ExportImportUtils.fullBackupToJson(
                    db.categoryDao().getAllCategories(),
                    db.bookDao().getAllBooks()
                )
                val newFile = destFolder.createFile("application/json", BackupFiles.dataFileName(vault.name, timestamp))
                if (newFile == null) {
                    allSucceeded = false
                    continue
                }
                context.contentResolver.openOutputStream(newFile.uri)?.use { it.write(json.toByteArray()) }
                    ?: run { allSucceeded = false }
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        // Only prune after a fully successful run, so a failed backup never costs you old ones.
        if (allSucceeded) {
            try {
                BackupFiles.pruneOldBackups(destFolder, BackupPrefs.getRetentionCount(context))
            } catch (e: Exception) {
                // Pruning is best-effort.
            }
        }

        BackupPrefs.recordBackupResult(context, success = allSucceeded)
        if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_MANUAL = "manual"
    }
}