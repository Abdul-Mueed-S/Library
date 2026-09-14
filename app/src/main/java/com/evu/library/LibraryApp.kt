package com.evu.library

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LibraryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BackupPrefs.isEnabled(this)) {
            scheduleBackupWork(this)
        }
    }

    companion object {
        private const val WORK_NAME = "library_periodic_backup"

        fun scheduleBackupWork(context: android.content.Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelBackupWork(context: android.content.Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}