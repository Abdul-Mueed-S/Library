package com.evu.library

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
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
            val vault = VaultManager.getAllVaults(this).find { it.id == vaultId }
            if (vault != null) promptForLocationNameThenSaveGps(vault)
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        val gpsLocationLabel = if (vault.latitude != null && !vault.isManualLocation) "Update Location (set to here)" else "Set Location (to here)"
        val manualLocationLabel = if (vault.latitude != null && vault.isManualLocation) "Edit Manual Location" else "Set Location Manually"
        val options = arrayOf("Rename", gpsLocationLabel, manualLocationLabel, "Delete")
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(vault.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(vault)
                    1 -> requestLocationForVault(vault)
                    2 -> showManualLocationDialog(vault)
                    3 -> confirmDeleteVault(vault)
                }
            }
            .show()
    }

    private fun requestLocationForVault(vault: LibraryVault) {
        if (LocationUtils.hasLocationPermission(this)) {
            promptForLocationNameThenSaveGps(vault)
        } else {
            pendingLocationVaultId = vault.id
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun promptForLocationNameThenSaveGps(vault: LibraryVault) {
        val input = EditText(this)
        input.hint = "Location name (optional, e.g. Home)"
        vault.locationName?.let { input.setText(it) }
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Name This Location")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { null }
                saveCurrentLocationForVault(vault.id, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCurrentLocationForVault(vaultId: String, locationName: String?) {
        val location = LocationUtils.getLastKnownLocation(this)
        if (location == null) {
            Toast.makeText(this, "Couldn't get current location — try again in a moment", Toast.LENGTH_SHORT).show()
            return
        }
        VaultManager.setVaultLocationFromGps(this, vaultId, location.latitude, location.longitude, locationName)
        Toast.makeText(this, "Location saved for this library", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun showManualLocationDialog(vault: LibraryVault) {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 0)

        val nameInput = EditText(this)
        nameInput.hint = "Location name (optional, e.g. Office)"
        vault.locationName?.let { nameInput.setText(it) }

        val latInput = EditText(this)
        latInput.hint = "Latitude"
        latInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        vault.latitude?.let { latInput.setText(it.toString()) }

        val lonInput = EditText(this)
        lonInput.hint = "Longitude"
        lonInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        vault.longitude?.let { lonInput.setText(it.toString()) }

        val radiusInput = EditText(this)
        radiusInput.hint = "Radius in meters (e.g. 150)"
        radiusInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        radiusInput.setText((vault.radiusMeters ?: LocationUtils.SUGGESTION_RADIUS_METERS).toString())

        container.addView(nameInput)
        container.addView(latInput)
        container.addView(lonInput)
        container.addView(radiusInput)

        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Manual Location for ${vault.name}")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val lat = latInput.text.toString().toDoubleOrNull()
                val lon = lonInput.text.toString().toDoubleOrNull()
                val radius = radiusInput.text.toString().toDoubleOrNull()
                if (lat == null || lon == null || radius == null) {
                    Toast.makeText(this, "Enter valid numbers for all three fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    Toast.makeText(this, "Latitude must be -90 to 90, longitude -180 to 180", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val name = nameInput.text.toString().ifBlank { null }
                VaultManager.setVaultLocationManual(this, vault.id, lat, lon, radius, name)
                Toast.makeText(this, "Manual location saved for ${vault.name}", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}