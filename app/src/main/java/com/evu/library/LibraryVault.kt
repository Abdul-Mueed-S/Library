package com.evu.library

data class LibraryVault(
    val id: String,
    val name: String,
    val dbFileName: String,
    val latitude: Double? = null,  // used later for location-based suggestions
    val longitude: Double? = null
)