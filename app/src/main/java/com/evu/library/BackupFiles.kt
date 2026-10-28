package com.evu.library

import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BackupKind { LIBRARY_DATA, VAULT_METADATA }

data class BackupEntry(
    val file: DocumentFile,
    val kind: BackupKind,
    val groupKey: String,
    val groupLabel: String,
    val timestamp: Date?
)

object BackupFiles {
    const val METADATA_LABEL = "Library names & locations"
    const val DATA_PREFIX = "library_backup_"

    private const val TIMESTAMP_PATTERN = "yyyy-MM-dd_HHmm"
    private val DATA_REGEX = Regex("""^(library_backup_.+)_(\d{4}-\d{2}-\d{2}_\d{4})\.json$""")
    private val METADATA_REGEX = Regex("""^(library_vaults_metadata)_(\d{4}-\d{2}-\d{2}_\d{4})\.json$""")

    fun newTimestamp(): String = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date())

    fun safeVaultName(name: String): String = name.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun dataFileName(vaultName: String, timestamp: String) =
        "$DATA_PREFIX${safeVaultName(vaultName)}_$timestamp.json"

    fun metadataFileName(timestamp: String) = "library_vaults_metadata_$timestamp.json"

    private fun parseTimestamp(value: String): Date? = try {
        SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).parse(value)
    } catch (e: Exception) {
        null
    }

    // Only files matching the app's own naming pattern are ever listed (and so ever pruned).
    fun listBackups(folder: DocumentFile): List<BackupEntry> {
        val result = mutableListOf<BackupEntry>()
        for (file in folder.listFiles()) {
            val name = file.name ?: continue
            val dataMatch = DATA_REGEX.matchEntire(name)
            val metadataMatch = METADATA_REGEX.matchEntire(name)
            when {
                dataMatch != null -> {
                    val key = dataMatch.groupValues[1]
                    result.add(
                        BackupEntry(
                            file, BackupKind.LIBRARY_DATA, key,
                            key.removePrefix(DATA_PREFIX).replace('_', ' '),
                            parseTimestamp(dataMatch.groupValues[2])
                        )
                    )
                }
                metadataMatch != null -> result.add(
                    BackupEntry(
                        file, BackupKind.VAULT_METADATA, metadataMatch.groupValues[1],
                        METADATA_LABEL, parseTimestamp(metadataMatch.groupValues[2])
                    )
                )
            }
        }
        return result.sortedByDescending { it.timestamp?.time ?: 0L }
    }

    // Keeps the newest `keep` files per group (each library, plus the metadata files).
    fun pruneOldBackups(folder: DocumentFile, keep: Int) {
        if (keep <= 0) return
        listBackups(folder).groupBy { it.groupKey }.values.forEach { group ->
            group.sortedByDescending { it.timestamp?.time ?: 0L }.drop(keep).forEach {
                try {
                    it.file.delete()
                } catch (e: Exception) {
                    // Some providers refuse deletes — leave the file, never fail the backup over it.
                }
            }
        }
    }
}