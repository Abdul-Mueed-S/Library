package com.evu.library

import android.content.Context

object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_NUMBERS = "show_numbers"
    private const val KEY_THEME = "selected_theme"

    const val THEME_DEFAULT = "default"
    const val THEME_LIGHT = "light"
    const val THEME_LIGHT_OCEAN = "light_ocean"
    const val THEME_LIGHT_FOREST = "light_forest"
    const val THEME_LIGHT_SUNSET = "light_sunset"
    const val THEME_TRUE_DARK = "true_dark"
    const val THEME_OCEAN = "ocean"
    const val THEME_FOREST = "forest"
    const val THEME_SUNSET = "sunset"
    const val THEME_MONOCHROME = "monochrome"

    fun isShowNumbers(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_NUMBERS, false)
    }

    fun setShowNumbers(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_NUMBERS, value).apply()
    }

    fun getSelectedTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT
    }

    fun setSelectedTheme(context: Context, themeKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, themeKey).apply()
    }

    fun getThemeStyleResId(themeKey: String): Int {
        return when (themeKey) {
            THEME_LIGHT -> R.style.Theme_Library_Light
            THEME_LIGHT_OCEAN -> R.style.Theme_Library_LightOcean
            THEME_LIGHT_FOREST -> R.style.Theme_Library_LightForest
            THEME_LIGHT_SUNSET -> R.style.Theme_Library_LightSunset
            THEME_TRUE_DARK -> R.style.Theme_Library_TrueDark
            THEME_OCEAN -> R.style.Theme_Library_Ocean
            THEME_FOREST -> R.style.Theme_Library_Forest
            THEME_SUNSET -> R.style.Theme_Library_Sunset
            THEME_MONOCHROME -> R.style.Theme_Library_Monochrome
            else -> R.style.Theme_Library_Default
        }
    }

    fun getThemeDisplayName(themeKey: String): String {
        return when (themeKey) {
            THEME_LIGHT -> "Light"
            THEME_LIGHT_OCEAN -> "Light Ocean"
            THEME_LIGHT_FOREST -> "Light Forest"
            THEME_LIGHT_SUNSET -> "Light Sunset"
            THEME_TRUE_DARK -> "True Dark"
            THEME_OCEAN -> "Ocean"
            THEME_FOREST -> "Forest"
            THEME_SUNSET -> "Sunset"
            THEME_MONOCHROME -> "Monochrome"
            else -> "Default (Purple)"
        }
    }

    fun getThemeSwatchColor(themeKey: String): Int {
        return when (themeKey) {
            THEME_LIGHT -> android.graphics.Color.parseColor("#7C4DFF")
            THEME_LIGHT_OCEAN -> android.graphics.Color.parseColor("#0288D1")
            THEME_LIGHT_FOREST -> android.graphics.Color.parseColor("#388E3C")
            THEME_LIGHT_SUNSET -> android.graphics.Color.parseColor("#F57C00")
            THEME_TRUE_DARK -> android.graphics.Color.parseColor("#00E5FF")
            THEME_OCEAN -> android.graphics.Color.parseColor("#4FC3F7")
            THEME_FOREST -> android.graphics.Color.parseColor("#81C784")
            THEME_SUNSET -> android.graphics.Color.parseColor("#FFB74D")
            THEME_MONOCHROME -> android.graphics.Color.parseColor("#B0B0B0")
            else -> android.graphics.Color.parseColor("#B085F5")
        }
    }

    fun getAllThemeKeys(): List<String> {
        return listOf(
            THEME_DEFAULT, THEME_LIGHT, THEME_LIGHT_OCEAN, THEME_LIGHT_FOREST, THEME_LIGHT_SUNSET,
            THEME_TRUE_DARK, THEME_OCEAN, THEME_FOREST, THEME_SUNSET, THEME_MONOCHROME
        )
    }
}