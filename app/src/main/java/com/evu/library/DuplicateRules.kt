package com.evu.library

object DuplicateRules {
    private fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")
    private fun isbnKey(s: String?) = s?.filter { it.isDigit() || it == 'X' || it == 'x' }?.uppercase().orEmpty()

    // "new" is the entry being saved, "old" is one already in the library.
    // New ISBN present: same ISBN = duplicate, anything else = not. No ISBN typed: the title decides.
    fun isDuplicate(newTitle: String, newIsbn: String?, oldTitle: String, oldIsbn: String?): Boolean {
        val a = isbnKey(newIsbn)
        if (a.isNotEmpty()) return a == isbnKey(oldIsbn)
        return norm(newTitle) == norm(oldTitle)
    }
}