package com.evu.library

data class LibraryVault(
    val id: String,
    val name: String,
    val dbFileName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double? = null,
    val isManualLocation: Boolean = false,
    val locationName: String? = null // friendly label, e.g. "Home", "Office"
)