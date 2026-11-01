package com.evu.library

// Declaration order = the order overlays are applied (background/surface/text first, so an
// explicit Text choice wins over the text colour a background sets).
enum class ColorRole(val prefKey: String, val label: String, val overlay: String) {
    BACKGROUND("background", "Screen background", "Ovr.Background"),
    SURFACE("surface", "Cards & dialogs", "Ovr.Surface"),
    TEXT("text", "Text", "Ovr.Text"),
    ACCENT("accent", "Accent (stripes, links, status bar)", "Ovr.Accent"),
    BUTTON("button", "Buttons", "Ovr.Button"),
    BADGE("badge", "Count badges", "Ovr.Badge"),
    DELETE("delete", "Delete & warnings", "Ovr.Delete")
}

data class PaletteColor(val key: String, val name: String, val hex: String)

object ThemePalettes {
    val UI_ORDER = listOf(
        ColorRole.ACCENT, ColorRole.BUTTON, ColorRole.BADGE, ColorRole.DELETE,
        ColorRole.BACKGROUND, ColorRole.SURFACE, ColorRole.TEXT
    )

    private val vivid = listOf(
        PaletteColor("Purple", "Purple", "#B085F5"), PaletteColor("Indigo", "Indigo", "#7C4DFF"),
        PaletteColor("Sky", "Sky", "#4FC3F7"), PaletteColor("Blue", "Blue", "#1E88E5"),
        PaletteColor("Cyan", "Cyan", "#00E5FF"), PaletteColor("Teal", "Teal", "#26A69A"),
        PaletteColor("Green", "Green", "#81C784"), PaletteColor("Lime", "Lime", "#C0CA33"),
        PaletteColor("Amber", "Amber", "#FFB74D"), PaletteColor("Orange", "Orange", "#F57C00"),
        PaletteColor("Red", "Red", "#E53935"), PaletteColor("Pink", "Pink", "#F06292")
    )

    private val deletes = listOf(
        PaletteColor("Red", "Red", "#E53935"), PaletteColor("Rose", "Rose", "#E57373"),
        PaletteColor("Coral", "Coral", "#FF7043"), PaletteColor("Magenta", "Magenta", "#D81B60"),
        PaletteColor("Orange", "Orange", "#FB8C00"), PaletteColor("Grey", "Grey", "#9E9E9E")
    )

    private val backgrounds = listOf(
        PaletteColor("Black", "Black", "#000000"), PaletteColor("Ink", "Ink", "#0D0D0D"),
        PaletteColor("Charcoal", "Charcoal", "#1C1C1C"), PaletteColor("Navy", "Navy", "#0F1A22"),
        PaletteColor("Forest", "Forest", "#132016"), PaletteColor("Plum", "Plum", "#1B1224"),
        PaletteColor("Cocoa", "Cocoa", "#221812"), PaletteColor("Paper", "Paper", "#ECE9F0"),
        PaletteColor("Cream", "Cream", "#FBEFE3"), PaletteColor("White", "White", "#FFFFFF")
    )

    private val surfaces = listOf(
        PaletteColor("Ink", "Ink", "#141414"), PaletteColor("Graphite", "Graphite", "#2A2A2A"),
        PaletteColor("Slate", "Slate", "#1E2A38"), PaletteColor("Moss", "Moss", "#1B2A1E"),
        PaletteColor("Plum", "Plum", "#2A1F33"), PaletteColor("Cocoa", "Cocoa", "#2E1F14"),
        PaletteColor("Mist", "Mist", "#E4E0EA"), PaletteColor("Sand", "Sand", "#F5E1CC"),
        PaletteColor("Snow", "Snow", "#FFFFFF")
    )

    private val texts = listOf(
        PaletteColor("White", "White", "#FFFFFF"), PaletteColor("Soft", "Soft white", "#E0E0E0"),
        PaletteColor("Warm", "Warm white", "#F5E9DD"), PaletteColor("Mint", "Mint", "#D4E8D5"),
        PaletteColor("Ice", "Ice", "#D0E6F5"), PaletteColor("Black", "Black", "#000000"),
        PaletteColor("Graphite", "Graphite", "#212121"), PaletteColor("Navy", "Navy", "#1A2B33")
    )

    fun forRole(role: ColorRole): List<PaletteColor> = when (role) {
        ColorRole.ACCENT, ColorRole.BUTTON, ColorRole.BADGE -> vivid
        ColorRole.DELETE -> deletes
        ColorRole.BACKGROUND -> backgrounds
        ColorRole.SURFACE -> surfaces
        ColorRole.TEXT -> texts
    }
}