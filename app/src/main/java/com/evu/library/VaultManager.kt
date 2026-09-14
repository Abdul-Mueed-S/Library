package com.evu.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object VaultManager {
    private const val PREFS_NAME = "vault_prefs"
    private const val KEY_VAULTS = "vaults_json"
    private const val KEY_ACTIVE_VAULT_ID = "active_vault_id"
    private const val LEGACY_DB_NAME = "library_database"
    private const val DEFAULT_VAULT_NAME = "My Library"

    fun getAllVaults(context: Context): List<LibraryVault> {
        ensureAtLeastOneVault(context)
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VAULTS, null) ?: return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<LibraryVault>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                LibraryVault(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    dbFileName = obj.getString("dbFileName"),
                    latitude = if (obj.isNull("latitude")) null else obj.getDouble("latitude"),
                    longitude = if (obj.isNull("longitude")) null else obj.getDouble("longitude"),
                    radiusMeters = if (!obj.has("radiusMeters") || obj.isNull("radiusMeters")) null else obj.getDouble("radiusMeters"),
                    isManualLocation = obj.optBoolean("isManualLocation", false),
                    locationName = if (!obj.has("locationName") || obj.isNull("locationName")) null else obj.getString("locationName")
                )
            )
        }
        return result
    }

    private fun saveAllVaults(context: Context, vaults: List<LibraryVault>) {
        val array = JSONArray()
        for (v in vaults) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("name", v.name)
            obj.put("dbFileName", v.dbFileName)
            obj.put("latitude", v.latitude ?: JSONObject.NULL)
            obj.put("longitude", v.longitude ?: JSONObject.NULL)
            obj.put("radiusMeters", v.radiusMeters ?: JSONObject.NULL)
            obj.put("isManualLocation", v.isManualLocation)
            obj.put("locationName", v.locationName ?: JSONObject.NULL)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VAULTS, array.toString()).apply()
    }

    private fun ensureAtLeastOneVault(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_VAULTS)) return

        val defaultVault = LibraryVault(
            id = UUID.randomUUID().toString(),
            name = DEFAULT_VAULT_NAME,
            dbFileName = LEGACY_DB_NAME
        )
        saveAllVaults(context, listOf(defaultVault))
        prefs.edit().putString(KEY_ACTIVE_VAULT_ID, defaultVault.id).apply()
    }

    fun getActiveVault(context: Context): LibraryVault {
        val vaults = getAllVaults(context)
        val activeId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_VAULT_ID, null)
        return vaults.find { it.id == activeId } ?: vaults.first()
    }

    fun setActiveVault(context: Context, vaultId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE_VAULT_ID, vaultId).apply()
    }

    fun createVault(context: Context, name: String): LibraryVault {
        val vaults = getAllVaults(context).toMutableList()
        val newVault = LibraryVault(
            id = UUID.randomUUID().toString(),
            name = name,
            dbFileName = "library_${UUID.randomUUID().toString().take(8)}.db"
        )
        vaults.add(newVault)
        saveAllVaults(context, vaults)
        return newVault
    }

    fun renameVault(context: Context, vaultId: String, newName: String) {
        val vaults = getAllVaults(context).map {
            if (it.id == vaultId) it.copy(name = newName) else it
        }
        saveAllVaults(context, vaults)
    }

    fun setVaultLocationFromGps(context: Context, vaultId: String, latitude: Double, longitude: Double, locationName: String?) {
        val vaults = getAllVaults(context).map {
            if (it.id == vaultId) it.copy(latitude = latitude, longitude = longitude, isManualLocation = false, locationName = locationName) else it
        }
        saveAllVaults(context, vaults)
    }

    fun setVaultLocationManual(context: Context, vaultId: String, latitude: Double, longitude: Double, radiusMeters: Double, locationName: String?) {
        val vaults = getAllVaults(context).map {
            if (it.id == vaultId) it.copy(latitude = latitude, longitude = longitude, radiusMeters = radiusMeters, isManualLocation = true, locationName = locationName) else it
        }
        saveAllVaults(context, vaults)
    }

    fun deleteVault(context: Context, vaultId: String) {
        val vaults = getAllVaults(context).filter { it.id != vaultId }
        saveAllVaults(context, vaults)
        val activeId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_VAULT_ID, null)
        if (activeId == vaultId && vaults.isNotEmpty()) {
            setActiveVault(context, vaults.first().id)
        }
    }

    fun exportVaultMetadataJson(context: Context): String {
        val vaults = getAllVaults(context)
        val array = JSONArray()
        for (v in vaults) {
            val obj = JSONObject()
            obj.put("name", v.name)
            obj.put("locationName", v.locationName ?: JSONObject.NULL)
            obj.put("latitude", v.latitude ?: JSONObject.NULL)
            obj.put("longitude", v.longitude ?: JSONObject.NULL)
            obj.put("radiusMeters", v.radiusMeters ?: JSONObject.NULL)
            obj.put("isManualLocation", v.isManualLocation)
            array.put(obj)
        }
        val root = JSONObject()
        root.put("formatVersion", 1)
        root.put("vaults", array)
        return root.toString(2)
    }

    // Restores vault names + locations from a metadata JSON (see exportVaultMetadataJson).
// Matches existing vaults by name (case-insensitive); creates a new vault for any
// name in the metadata that doesn't already exist. Does NOT touch book/category data —
// that's restored separately per-vault via the existing full-backup restore flow.
    fun restoreVaultMetadataJson(context: Context, json: String): Int {
        val root = JSONObject(json)
        val array = root.optJSONArray("vaults") ?: return 0
        var restoredCount = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val locationName = if (obj.isNull("locationName")) null else obj.getString("locationName")
            val latitude = if (obj.isNull("latitude")) null else obj.getDouble("latitude")
            val longitude = if (obj.isNull("longitude")) null else obj.getDouble("longitude")
            val radiusMeters = if (!obj.has("radiusMeters") || obj.isNull("radiusMeters")) null else obj.getDouble("radiusMeters")
            val isManual = obj.optBoolean("isManualLocation", false)

            val existing = getAllVaults(context).find { it.name.equals(name, ignoreCase = true) }
            val targetVault = existing ?: createVault(context, name)

            if (latitude != null && longitude != null) {
                val vaults = getAllVaults(context).map {
                    if (it.id == targetVault.id) it.copy(
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radiusMeters,
                        isManualLocation = isManual,
                        locationName = locationName
                    ) else it
                }
                saveAllVaults(context, vaults)
            }
            restoredCount++
        }
        return restoredCount
    }
}