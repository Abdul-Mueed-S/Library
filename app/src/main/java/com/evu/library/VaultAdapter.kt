package com.evu.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VaultAdapter(
    private var vaults: List<LibraryVault>,
    private var activeVaultId: String,
    private val onSwitch: (LibraryVault) -> Unit,
    private val onOptions: (LibraryVault) -> Unit
) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    class VaultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.vaultNameText)
        val location: TextView = view.findViewById(R.id.vaultLocationText)
        val activeLabel: TextView = view.findViewById(R.id.vaultActiveText)
        val options: TextView = view.findViewById(R.id.vaultOptionsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vault, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val vault = vaults[position]
        holder.name.text = vault.name
        holder.activeLabel.visibility = if (vault.id == activeVaultId) View.VISIBLE else View.GONE

        if (vault.locationName != null) {
            holder.location.text = "📍 ${vault.locationName}"
            holder.location.visibility = View.VISIBLE
        } else if (vault.latitude != null) {
            holder.location.text = "📍 Location set (unnamed)"
            holder.location.visibility = View.VISIBLE
        } else {
            holder.location.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onSwitch(vault) }
        holder.options.setOnClickListener { onOptions(vault) }
    }

    override fun getItemCount() = vaults.size

    fun updateList(newVaults: List<LibraryVault>, newActiveId: String) {
        vaults = newVaults
        activeVaultId = newActiveId
        notifyDataSetChanged()
    }
}