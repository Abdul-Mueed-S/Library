package com.evu.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.documentfile.provider.DocumentFile

class SettingsActivity : BaseActivity() {

    private val chooseFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            BackupPrefs.setDestinationUri(this, uri.toString())

            // Google Drive (and most SAF providers) expose folders as opaque content
            // URIs with no real filesystem path and no reliable way to read the signed-in
            // account's email through this API — so the closest identifying info available
            // is the folder's own display name plus a short form of its document ID.
            val folder = DocumentFile.fromTreeUri(this, uri)
            val folderName = folder?.name ?: "Selected folder"
            val idFragment = uri.lastPathSegment?.substringAfterLast(':')?.take(24) ?: ""
            val label = if (idFragment.isNotBlank()) "$folderName ($idFragment)" else folderName
            BackupPrefs.setDestinationLabel(this, label)

            updateBackupLabels()
            Toast.makeText(this, "Backup folder set", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        findViewById<android.widget.LinearLayout>(R.id.librariesRow).setOnClickListener {
            startActivity(Intent(this, LibrariesActivity::class.java))
        }

        findViewById<android.widget.LinearLayout>(R.id.exportImportRow).setOnClickListener {
            startActivity(Intent(this, ImportExportActivity::class.java))
        }

        val numberedSwitch = findViewById<SwitchCompat>(R.id.numberedListSwitch)
        numberedSwitch.isChecked = AppPrefs.isShowNumbers(this)
        numberedSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppPrefs.setShowNumbers(this, isChecked)
        }

        val backupSwitch = findViewById<SwitchCompat>(R.id.backupEnabledSwitch)
        backupSwitch.isChecked = BackupPrefs.isEnabled(this)
        backupSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && BackupPrefs.getDestinationUri(this) == null) {
                Toast.makeText(this, "Choose a backup folder first", Toast.LENGTH_SHORT).show()
                backupSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            BackupPrefs.setEnabled(this, isChecked)
            if (isChecked) {
                LibraryApp.scheduleBackupWork(this)
            } else {
                LibraryApp.cancelBackupWork(this)
            }
        }

        findViewById<android.widget.Button>(R.id.chooseBackupFolderButton).setOnClickListener {
            chooseFolderLauncher.launch(null)
        }

        findViewById<android.widget.Button>(R.id.backupNowButton).setOnClickListener {
            runManualBackupNow()
        }

        updateLabels()
        updateBackupLabels()

        findViewById<android.widget.LinearLayout>(R.id.themesRow).setOnClickListener {
            startActivity(Intent(this, ThemesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateLabels()
        updateBackupLabels()
    }

    private fun updateLabels() {
        val currentTheme = AppPrefs.getThemeDisplayName(AppPrefs.getSelectedTheme(this))
        findViewById<android.widget.TextView>(R.id.currentThemeText).text = currentTheme

        val currentVault = VaultManager.getActiveVault(this)
        findViewById<android.widget.TextView>(R.id.currentLibraryText).text = currentVault.name
    }

    private fun updateBackupLabels() {
        val destUri = BackupPrefs.getDestinationUri(this)
        val destLabel = BackupPrefs.getDestinationLabel(this)
        findViewById<android.widget.TextView>(R.id.backupFolderStatusText).text = when {
            destUri == null -> "No folder chosen yet"
            destLabel != null -> "Folder set: $destLabel"
            else -> "Folder set"
        }

        val lastTime = BackupPrefs.getLastBackupTime(this)
        val lastStatusText = if (lastTime < 0) {
            "No backup has run yet"
        } else {
            val formatted = DateFormat.format("MMM d, yyyy h:mm a", lastTime)
            val result = if (BackupPrefs.getLastBackupSuccess(this)) "Succeeded" else "Failed"
            "Last backup: $formatted — $result"
        }
        findViewById<android.widget.TextView>(R.id.lastBackupStatusText).text = lastStatusText
    }

    private fun runManualBackupNow() {
        if (BackupPrefs.getDestinationUri(this) == null) {
            Toast.makeText(this, "Choose a backup folder first", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Running backup...", Toast.LENGTH_SHORT).show()
        val request = androidx.work.OneTimeWorkRequestBuilder<BackupWorker>().build()
        androidx.work.WorkManager.getInstance(this).enqueue(request)
        androidx.work.WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    updateBackupLabels()
                }
            }
    }
}