package com.evu.library

object Utils {
    fun clean(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    fun toTitleCase(text: String): String {
        val cleaned = clean(text)
        if (cleaned.isEmpty()) return cleaned
        return cleaned.split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word[0].uppercaseChar() + word.substring(1).lowercase()
        }
    }

    fun formatIsbn(digits: String): String = digits.filter { it.isDigit() }.take(13)

    fun isDuplicate(title1: String, edition1: String?, title2: String, edition2: String?): Boolean {
        if (!clean(title1).equals(clean(title2), ignoreCase = true)) return false
        val e1 = edition1?.trim().orEmpty()
        val e2 = edition2?.trim().orEmpty()
        if (e1.isEmpty() || e2.isEmpty()) return true
        return e1.equals(e2, ignoreCase = true)
    }
}