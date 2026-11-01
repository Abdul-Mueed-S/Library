package com.evu.library

import android.app.Activity
import android.content.Context
import android.content.res.Configuration

object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SHOW_NUMBERS = "show_numbers"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_THEME_VERSION = "theme_version"
    private const val KEY_AUTO = "auto_mode"
    private const val KEY_AUTO_DARK = "auto_dark_theme"
    private const val KEY_AUTO_LIGHT = "auto_light_theme"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_CORNERS = "corner_style"
    private const val KEY_COMPACT = "compact_cards"
    private const val KEY_ANIMATIONS = "animations"
    private const val KEY_ROLE_PREFIX = "role_"

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

    const val CORNERS_SHARP = "sharp"
    const val CORNERS_SOFT = "soft"
    const val CORNERS_ROUND = "round"

    val TEXT_SCALE_KEYS = listOf("small", "normal", "large", "xlarge")

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Existing settings ----------
    fun isShowNumbers(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_NUMBERS, false)

    fun setShowNumbers(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_NUMBERS, value).apply()
    }

    // ---------- Base theme ----------
    fun getSelectedTheme(context: Context): String = prefs(context).getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT

    fun setSelectedTheme(context: Context, themeKey: String) {
        prefs(context).edit()
            .putString(KEY_THEME, themeKey)
            .putString(if (isLightTheme(themeKey)) KEY_AUTO_LIGHT else KEY_AUTO_DARK, themeKey)
            .apply()
    }

    fun isLightTheme(key: String) =
        key == THEME_LIGHT || key == THEME_LIGHT_OCEAN || key == THEME_LIGHT_FOREST || key == THEME_LIGHT_SUNSET

    // ---------- Auto light/dark ----------
    fun isAutoMode(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, false)

    fun setAutoMode(context: Context, value: Boolean) {
        val editor = prefs(context).edit().putBoolean(KEY_AUTO, value)
        if (value) {
            val selected = getSelectedTheme(context)
            editor.putString(if (isLightTheme(selected)) KEY_AUTO_LIGHT else KEY_AUTO_DARK, selected)
        }
        editor.apply()
    }

    fun getAutoDarkTheme(context: Context): String = prefs(context).getString(KEY_AUTO_DARK, THEME_DEFAULT) ?: THEME_DEFAULT
    fun getAutoLightTheme(context: Context): String = prefs(context).getString(KEY_AUTO_LIGHT, THEME_LIGHT) ?: THEME_LIGHT

    fun getEffectiveThemeKey(context: Context): String {
        if (!isAutoMode(context)) return getSelectedTheme(context)
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return if (night) getAutoDarkTheme(context) else getAutoLightTheme(context)
    }

    // ---------- Per-element colours ----------
    fun getRoleChoice(context: Context, role: ColorRole): String? =
        prefs(context).getString(KEY_ROLE_PREFIX + role.prefKey, null)

    fun setRoleChoice(context: Context, role: ColorRole, key: String?) {
        val editor = prefs(context).edit()
        if (key == null) editor.remove(KEY_ROLE_PREFIX + role.prefKey) else editor.putString(KEY_ROLE_PREFIX + role.prefKey, key)
        editor.apply()
    }

    // ---------- Shape, text, density, motion ----------
    fun getCorners(context: Context): String = prefs(context).getString(KEY_CORNERS, CORNERS_SOFT) ?: CORNERS_SOFT
    fun setCorners(context: Context, value: String) { prefs(context).edit().putString(KEY_CORNERS, value).apply() }

    fun getTextScaleKey(context: Context): String = prefs(context).getString(KEY_TEXT_SCALE, "normal") ?: "normal"
    fun setTextScaleKey(context: Context, key: String) { prefs(context).edit().putString(KEY_TEXT_SCALE, key).apply() }

    fun getTextScale(context: Context): Float = when (getTextScaleKey(context)) {
        "small" -> 0.9f
        "large" -> 1.15f
        "xlarge" -> 1.3f
        else -> 1.0f
    }

    fun textScaleLabel(key: String) = when (key) {
        "small" -> "Small"
        "large" -> "Large"
        "xlarge" -> "Extra large"
        else -> "Default"
    }

    fun isCompact(context: Context): Boolean = prefs(context).getBoolean(KEY_COMPACT, false)
    fun setCompact(context: Context, value: Boolean) { prefs(context).edit().putBoolean(KEY_COMPACT, value).apply() }

    fun isAnimationsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ANIMATIONS, true)
    fun setAnimationsEnabled(context: Context, value: Boolean) { prefs(context).edit().putBoolean(KEY_ANIMATIONS, value).apply() }

    // ---------- Applying ----------
    // Bumped whenever something visual changes; screens that were already open see it in onResume and refresh.
    fun getThemeVersion(context: Context): Int = prefs(context).getInt(KEY_THEME_VERSION, 0)
    fun bumpThemeVersion(context: Context) {
        prefs(context).edit().putInt(KEY_THEME_VERSION, getThemeVersion(context) + 1).apply()
    }

    fun applyTheme(activity: Activity) {
        activity.setTheme(getThemeStyleResId(getEffectiveThemeKey(activity)))
        val theme = activity.theme
        val resources = activity.resources
        val pkg = activity.packageName

        fun applyOverlay(name: String) {
            val id = resources.getIdentifier(name, "style", pkg).takeIf { it != 0 }
                ?: resources.getIdentifier(name.replace('.', '_'), "style", pkg)
            if (id != 0) theme.applyStyle(id, true)
        }

        for (role in ColorRole.values()) {
            getRoleChoice(activity, role)?.let { applyOverlay("${role.overlay}.$it") }
        }
        when (getCorners(activity)) {
            CORNERS_SHARP -> applyOverlay("Ovr.Corner.Sharp")
            CORNERS_ROUND -> applyOverlay("Ovr.Corner.Round")
        }
    }

    // Clears colours, corners, text size and density. The base theme and the animation switch stay as they are.
    fun resetCustomizations(context: Context) {
        val editor = prefs(context).edit()
        ColorRole.values().forEach { editor.remove(KEY_ROLE_PREFIX + it.prefKey) }
        editor.remove(KEY_CORNERS).remove(KEY_TEXT_SCALE).remove(KEY_COMPACT)
        editor.apply()
    }

    // ---------- Base theme helpers (unchanged) ----------
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