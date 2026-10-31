package com.evu.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SnapshotInfo(
    val key: String,
    val name: String,
    val createdAt: Long,
    val libraryCount: Int, // -1 = unknown (copies that exist only in the backup folder)
    val bookCount: Int,
    val localId: String?,
    val folderUri: String?
) {
    val onPhone: Boolean get() = localId != null
    val inFolder: Boolean get() = folderUri != null
}

data class SnapshotCreateResult(val info: SnapshotInfo, val folderConfigured: Boolean, val savedToFolder: Boolean)
data class SnapshotRestoreResult(val restored: Int, val total: Int)

// A snapshot is a manual, point-in-time copy of EVERY library (books incl. trash, categories,
// names and locations). Kept on the phone and, when a backup folder is set, also copied there.
object SnapshotManager {
    private const val PREFS_NAME = "snapshot_prefs"
    private const val KEY_INDEX = "index_all"
    private const val TIMESTAMP_PATTERN = "yyyy-MM-dd_HHmm"
    private val FOLDER_FILE_REGEX =
        Regex("""^library_snapshot_(\d{4}-\d{2}-\d{2}_\d{4})_([0-9a-f]{8})_(.*)\.json$""")

    private class FolderEntry(val id8: String, val file: DocumentFile, val createdAt: Long, val name: String)

    private class ParsedLibrary(
        val id: String,
        val name: String,
        val locationName: String?,
        val latitude: Double?,
        val longitude: Double?,
        val radiusMeters: Double?,
        val isManual: Boolean,
        val data: FullBackupData
    )

    private fun dir(context: Context) = File(context.filesDir, "snapshots")

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readLocalIndex(context: Context): List<SnapshotInfo> {
        val json = prefs(context).getString(KEY_INDEX, null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<SnapshotInfo>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val id = o.getString("id")
            result.add(
                SnapshotInfo(
                    key = id, name = o.getString("name"), createdAt = o.getLong("createdAt"),
                    libraryCount = o.getInt("libraryCount"), bookCount = o.getInt("bookCount"),
                    localId = id, folderUri = null
                )
            )
        }
        return result
    }

    private fun writeLocalIndex(context: Context, items: List<SnapshotInfo>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject().put("id", it.localId ?: it.key).put("name", it.name)
                    .put("createdAt", it.createdAt).put("libraryCount", it.libraryCount)
                    .put("bookCount", it.bookCount)
            )
        }
        prefs(context).edit().putString(KEY_INDEX, array.toString()).apply()
    }

    private fun backupFolder(context: Context): DocumentFile? {
        val uriString = BackupPrefs.getDestinationUri(context) ?: return null
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(uriString)) ?: return null
        return if (folder.canRead()) folder else null
    }

    private fun safeSegment(name: String): String =
        name.replace(Regex("[^A-Za-z0-9-]"), "_").take(40).ifBlank { "snapshot" }

    private fun folderFileName(id: String, createdAt: Long, name: String): String {
        val stamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date(createdAt))
        return "library_snapshot_${stamp}_${id.take(8)}_${safeSegment(name)}.json"
    }

    private fun listFolderEntries(context: Context): List<FolderEntry> {
        val folder: DocumentFile? = try {
            backupFolder(context)
        } catch (e: Exception) {
            null
        }
        if (folder == null) return emptyList()
        val format = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
        val result = mutableListOf<FolderEntry>()
        try {
            for (file in folder.listFiles()) {
                val match = FOLDER_FILE_REGEX.matchEntire(file.name ?: continue) ?: continue
                val createdAt = try {
                    format.parse(match.groupValues[1])?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
                val name = match.groupValues[3].replace('_', ' ').trim().ifEmpty { "Snapshot" }
                result.add(FolderEntry(match.groupValues[2], file, createdAt, name))
            }
        } catch (e: Exception) {
            // Provider hiccup — show what we have.
        }
        return result
    }

    // Phone copies merged with whatever is in the backup folder (newest first).
    suspend fun listAll(context: Context): List<SnapshotInfo> = withContext(Dispatchers.IO) {
        val local = readLocalIndex(context)
        val folderEntries = listFolderEntries(context)

        val result = mutableListOf<SnapshotInfo>()
        for (item in local) {
            val match = folderEntries.find { it.id8 == (item.localId ?: "").take(8) }
            result.add(item.copy(folderUri = match?.file?.uri?.toString()))
        }
        for (entry in folderEntries) {
            if (local.none { (it.localId ?: "").take(8) == entry.id8 }) {
                result.add(
                    SnapshotInfo(
                        key = "folder:${entry.id8}", name = entry.name, createdAt = entry.createdAt,
                        libraryCount = -1, bookCount = -1, localId = null,
                        folderUri = entry.file.uri.toString()
                    )
                )
            }
        }
        result.sortedByDescending { it.createdAt }
    }

    suspend fun create(context: Context, name: String?): SnapshotCreateResult = withContext(Dispatchers.IO) {
        val vaults = VaultManager.getAllVaults(context)
        val libraries = JSONArray()
        var totalBooks = 0
        for (vault in vaults) {
            val db = AppDatabase.getDatabaseForVault(context, vault)
            val books = db.bookDao().getEveryBook() // includes Trash, so a restore is exact
            val categories = db.categoryDao().getAllCategories()
            totalBooks += books.size
            libraries.put(
                JSONObject()
                    .put("id", vault.id)
                    .put("name", vault.name)
                    .put("locationName", vault.locationName ?: JSONObject.NULL)
                    .put("latitude", vault.latitude ?: JSONObject.NULL)
                    .put("longitude", vault.longitude ?: JSONObject.NULL)
                    .put("radiusMeters", vault.radiusMeters ?: JSONObject.NULL)
                    .put("isManualLocation", vault.isManualLocation)
                    .put("data", JSONObject(ExportImportUtils.fullBackupToJson(categories, books)))
            )
        }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val label = name?.trim()?.takeIf { it.isNotEmpty() } ?: "Snapshot"
        val text = JSONObject()
            .put("snapshotFormat", 1)
            .put("id", id)
            .put("name", label)
            .put("createdAt", now)
            .put("libraries", libraries)
            .toString()

        // 1) Phone copy
        val snapshotsDir = dir(context)
        snapshotsDir.mkdirs()
        File(snapshotsDir, "$id.json").writeText(text)

        // 2) Backup-folder copy (best effort)
        val folderConfigured = BackupPrefs.getDestinationUri(context) != null
        var folderUri: String? = null
        if (folderConfigured) {
            try {
                val folder = backupFolder(context)
                if (folder != null && folder.canWrite()) {
                    val file = folder.createFile("application/json", folderFileName(id, now, label))
                    if (file != null) {
                        context.contentResolver.openOutputStream(file.uri)?.use { it.write(text.toByteArray()) }
                        folderUri = file.uri.toString()
                    }
                }
            } catch (e: Exception) {
                folderUri = null
            }
        }

        val info = SnapshotInfo(id, label, now, vaults.size, totalBooks, id, folderUri)
        writeLocalIndex(context, readLocalIndex(context) + info)
        SnapshotCreateResult(info, folderConfigured, folderUri != null)
    }

    private fun readText(context: Context, info: SnapshotInfo): String? {
        info.localId?.let { id ->
            val file = File(dir(context), "$id.json")
            if (file.exists()) return file.readText()
        }
        info.folderUri?.let { uri ->
            return try {
                context.contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parse(text: String): List<ParsedLibrary>? {
        try {
            val array = JSONObject(text).getJSONArray("libraries")
            val result = mutableListOf<ParsedLibrary>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val data = ExportImportUtils.parseFullBackupJson(o.getJSONObject("data").toString()) ?: return null
                result.add(
                    ParsedLibrary(
                        id = o.optString("id", ""),
                        name = o.getString("name"),
                        locationName = o.optString("locationName", null).takeIf { it != "null" },
                        latitude = if (o.isNull("latitude")) null else o.getDouble("latitude"),
                        longitude = if (o.isNull("longitude")) null else o.getDouble("longitude"),
                        radiusMeters = if (o.isNull("radiusMeters")) null else o.getDouble("radiusMeters"),
                        isManual = o.optBoolean("isManualLocation", false),
                        data = data
                    )
                )
            }
            return result
        } catch (e: Exception) {
            return null
        }
    }

    // Returns null when the file can't be read or isn't a valid snapshot (nothing is changed).
    suspend fun restore(context: Context, info: SnapshotInfo): SnapshotRestoreResult? = withContext(Dispatchers.IO) {
        val text = readText(context, info) ?: return@withContext null
        val libraries = parse(text) ?: return@withContext null // validate everything BEFORE touching any library

        var restored = 0
        for (lib in libraries) {
            try {
                val vaults = VaultManager.getAllVaults(context)
                val vault = vaults.find { it.id == lib.id }
                    ?: vaults.find { it.name.equals(lib.name, ignoreCase = true) }
                    ?: VaultManager.createVault(context, lib.name)

                if (lib.latitude != null && lib.longitude != null) {
                    if (lib.isManual && lib.radiusMeters != null) {
                        VaultManager.setVaultLocationManual(context, vault.id, lib.latitude, lib.longitude, lib.radiusMeters, lib.locationName)
                    } else {
                        VaultManager.setVaultLocationFromGps(context, vault.id, lib.latitude, lib.longitude, lib.locationName)
                    }
                }
                BackupRestorer.restoreIntoDatabase(AppDatabase.getDatabaseForVault(context, vault), lib.data)
                restored++
            } catch (e: Exception) {
                // This library failed (its own transaction rolled back); carry on with the rest.
            }
        }
        SnapshotRestoreResult(restored, libraries.size)
    }

    // Removes the phone copy and the backup-folder copy.
    suspend fun delete(context: Context, info: SnapshotInfo) {
        withContext(Dispatchers.IO) {
            info.localId?.let { id ->
                File(dir(context), "$id.json").delete()
                writeLocalIndex(context, readLocalIndex(context).filter { it.localId != id })
            }
            info.folderUri?.let { uri ->
                try {
                    DocumentFile.fromSingleUri(context, Uri.parse(uri))?.delete()
                } catch (e: Exception) {
                    // Some providers refuse deletes.
                }
            }
        }
    }
}