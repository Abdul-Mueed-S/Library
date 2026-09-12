package com.evu.library

object ImportHolder {
    var books: MutableList<ImportedBook> = mutableListOf()
    var defaultIncludeCategories: Boolean = true
    var defaultIncludeFavorites: Boolean = true
    var defaultIncludeDuplicateFlags: Boolean = false
}