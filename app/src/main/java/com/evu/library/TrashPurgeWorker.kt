package com.evu.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Permanently removes anything that has sat in Trash longer than RETENTION_DAYS,
// across every library.
class TrashPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * DAY_MS
        for (vault in VaultManager.getAllVaults(applicationContext)) {
            try {
                AppDatabase.getDatabaseForVault(applicationContext, vault).bookDao().purgeTrashedBefore(cutoff)
            } catch (e: Exception) {
                // One library failing must not stop the others.
            }
        }
        return Result.success()
    }

    companion object {
        const val RETENTION_DAYS = 30L
        const val DAY_MS = 86_400_000L
    }
}