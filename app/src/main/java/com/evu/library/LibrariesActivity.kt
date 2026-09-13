package com.evu.library

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LibrariesActivity : BaseActivity() {

    private lateinit var adapter: VaultAdapter

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
        // Restart the whole task fresh so every activity picks up the new
        // active database, same pattern used for theme switching.
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
        val options = arrayOf("Rename", "Delete")
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(vault.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(vault)
                    1 -> confirmDeleteVault(vault)
                }
            }
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
                } else {
                    refresh()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}