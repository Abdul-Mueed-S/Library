package com.evu.library

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LibraryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleTrashPurge(this)
        if (BackupPrefs.isEnabled(this)) {
            scheduleBackupWork(this)
        }
    }

    companion object {
        private const val BACKUP_WORK_NAME = "library_periodic_backup"
        private const val PURGE_WORK_NAME = "library_trash_purge"

        fun scheduleBackupWork(context: Context) {
            val minutes = BackupPrefs.getIntervalMinutes(context)
            val request = PeriodicWorkRequestBuilder<BackupWorker>(minutes, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BACKUP_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }

        fun cancelBackupWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
        }

        private fun scheduleTrashPurge(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PURGE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}