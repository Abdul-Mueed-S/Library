package com.evu.library

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LibrariesActivity : BaseActivity() {

    private lateinit var adapter: VaultAdapter
    private var pendingLocationVaultId: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val vaultId = pendingLocationVaultId
        pendingLocationVaultId = null
        if (granted && vaultId != null) {
            saveCurrentLocationForVault(vaultId)
        } else if (!granted) {
            Toast.makeText(this, "Location permission needed to set a library's location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libraries)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.vaultsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VaultAdapter(
            emptyList(),
            "",
            onSwitch = { vault -> switchToVault(vault) },
            onOptions = { vault -> showVaultOptions(vault) }
        )
        recyclerView.adapter = adapter

        findViewById<android.widget.Button>(R.id.addLibraryButton).setOnClickListener {
            showCreateVaultDialog()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val vaults = VaultManager.getAllVaults(this)
        val active = VaultManager.getActiveVault(this)
        adapter.updateList(vaults, active.id)
    }

    private fun switchToVault(vault: LibraryVault) {
        val current = VaultManager.getActiveVault(this)
        if (vault.id == current.id) return

        VaultManager.setActiveVault(this, vault.id)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showCreateVaultDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("New Library")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = Utils.toTitleCase(input.text.toString())
                if (name.isNotEmpty()) {
                    VaultManager.createVault(this, name)
                    refresh()
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVaultOptions(vault: LibraryVault) {
        val locationLabel = if (vault.latitude != null) "Update Location (set to here)" else "Set Location (to here)"
        val options = arrayOf("Rename", locationLabel, "Delete")
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(vault.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(vault)
                    1 -> requestLocationForVault(vault)
                    2 -> confirmDeleteVault(vault)
                }
            }
            .show()
    }

    private fun requestLocationForVault(vault: LibraryVault) {
        if (LocationUtils.hasLocationPermission(this)) {
            saveCurrentLocationForVault(vault.id)
        } else {
            pendingLocationVaultId = vault.id
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun saveCurrentLocationForVault(vaultId: String) {
        val location = LocationUtils.getLastKnownLocation(this)
        if (location == null) {
            Toast.makeText(this, "Couldn't get current location — try again in a moment", Toast.LENGTH_SHORT).show()
            return
        }
        VaultManager.setVaultLocation(this, vaultId, location.latitude, location.longitude)
        Toast.makeText(this, "Location saved for this library", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun showRenameDialog(vault: LibraryVault) {
        val input = EditText(this)
        input.setText(vault.name)
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Rename Library")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = Utils.toTitleCase(input.text.toString())
                if (newName.isNotEmpty()) {
                    VaultManager.renameVault(this, vault.id, newName)
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteVault(vault: LibraryVault) {
        val allVaults = VaultManager.getAllVaults(this)
        if (allVaults.size <= 1) {
            Toast.makeText(this, "Can't delete your only library", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Delete Library")
            .setMessage("Delete \"${vault.name}\"? All books and categories in it will be permanently deleted. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val wasActive = VaultManager.getActiveVault(this).id == vault.id
                AppDatabase.closeInstance(vault.dbFileName)
                deleteDatabase(vault.dbFileName)
                VaultManager.deleteVault(this, vault.id)
                if (wasActive) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}