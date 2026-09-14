package com.evu.library

import android.content.Context

object BackupPrefs {
    private const val PREFS_NAME = "backup_prefs"
    private const val KEY_DEST_URI = "backup_dest_uri"
    private const val KEY_DEST_LABEL = "backup_dest_label"
    private const val KEY_ENABLED = "backup_enabled"
    private const val KEY_LAST_TIME = "last_backup_time"
    private const val KEY_LAST_SUCCESS = "last_backup_success"
    private const val KEY_INTERVAL_TYPE = "backup_interval_type"
    private const val KEY_CUSTOM_VALUE = "backup_custom_value"
    private const val KEY_CUSTOM_UNIT = "backup_custom_unit"

    const val INTERVAL_DAY = "1_day"
    const val INTERVAL_WEEK = "7_days"
    const val INTERVAL_MONTH = "1_month"
    const val INTERVAL_CUSTOM = "custom"

    const val UNIT_HOURS = "hours"
    const val UNIT_DAYS = "days"
    const val UNIT_WEEKS = "weeks"
    const val UNIT_MONTHS = "months"
    const val UNIT_YEARS = "years"

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

    fun getIntervalType(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_INTERVAL_TYPE, INTERVAL_DAY) ?: INTERVAL_DAY

    fun getCustomValue(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_CUSTOM_VALUE, 1)

    fun getCustomUnit(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CUSTOM_UNIT, UNIT_DAYS) ?: UNIT_DAYS

    fun setPresetInterval(context: Context, type: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_INTERVAL_TYPE, type)
            .apply()
    }

    fun setCustomInterval(context: Context, value: Int, unit: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_INTERVAL_TYPE, INTERVAL_CUSTOM)
            .putInt(KEY_CUSTOM_VALUE, value)
            .putString(KEY_CUSTOM_UNIT, unit)
            .apply()
    }

    private fun unitToMinutes(unit: String): Long = when (unit) {
        UNIT_HOURS -> 60L
        UNIT_DAYS -> 1440L
        UNIT_WEEKS -> 10080L
        UNIT_MONTHS -> 43200L  // approximated as 30 days
        UNIT_YEARS -> 525600L  // approximated as 365 days
        else -> 1440L
    }

    // WorkManager enforces a 15-minute minimum on periodic work regardless — this
    // clamp just ensures our own display/logic agrees with that reality.
    fun getIntervalMinutes(context: Context): Long {
        val minutes = when (getIntervalType(context)) {
            INTERVAL_DAY -> 1440L
            INTERVAL_WEEK -> 10080L
            INTERVAL_MONTH -> 43200L
            INTERVAL_CUSTOM -> getCustomValue(context).toLong() * unitToMinutes(getCustomUnit(context))
            else -> 1440L
        }
        return minutes.coerceAtLeast(15L)
    }

    fun getIntervalDisplayText(context: Context): String {
        return when (getIntervalType(context)) {
            INTERVAL_DAY -> "Every 1 day"
            INTERVAL_WEEK -> "Every 7 days"
            INTERVAL_MONTH -> "Every 1 month (~30 days)"
            INTERVAL_CUSTOM -> {
                val value = getCustomValue(context)
                val unit = getCustomUnit(context)
                val unitLabel = when (unit) {
                    UNIT_HOURS -> if (value == 1) "hour" else "hours"
                    UNIT_DAYS -> if (value == 1) "day" else "days"
                    UNIT_WEEKS -> if (value == 1) "week" else "weeks"
                    UNIT_MONTHS -> if (value == 1) "month" else "months"
                    UNIT_YEARS -> if (value == 1) "year" else "years"
                    else -> unit
                }
                "Every $value $unitLabel"
            }
            else -> "Every 1 day"
        }
    }
}