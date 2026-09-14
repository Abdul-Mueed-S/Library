package com.evu.library

import android.content.Context

object BackupPrefs {
    private const val PREFS_NAME = "backup_prefs"
    private const val KEY_DEST_URI = "backup_dest_uri"
    private const val KEY_DEST_LABEL = "backup_dest_label"
    private const val KEY_ENABLED = "backup_enabled"
    private const val KEY_LAST_TIME = "last_backup_time"
    private const val KEY_LAST_SUCCESS = "last_backup_success"

    fun getDestinationUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DEST_URI, null)

    fun setDestinationUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_DEST_URI, uri).apply()
    }

    fun getDestinationLabel(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DEST_LABEL, null)

    fun setDestinationLabel(context: Context, label: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_DEST_LABEL, label).apply()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun recordBackupResult(context: Context, success: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .putBoolean(KEY_LAST_SUCCESS, success)
            .apply()
    }

    fun getLastBackupTime(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_TIME, -1L)

    fun getLastBackupSuccess(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_LAST_SUCCESS, false)
}