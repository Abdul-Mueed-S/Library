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
        if (BackupPrefs.isEnabled(this)) {
            scheduleBackupWork(this)
        }
    }

    companion object {
        private const val WORK_NAME = "library_periodic_backup"

        fun scheduleBackupWork(context: Context) {
            val minutes = BackupPrefs.getIntervalMinutes(context)
            val request = PeriodicWorkRequestBuilder<BackupWorker>(minutes, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelBackupWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}