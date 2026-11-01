package com.evu.library

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ThemesActivity : BaseActivity() {

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun resolveColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    // Every change bumps the theme version (so other open screens refresh on return) and rebuilds this screen.
    private fun applyChange() {
        AppPrefs.bumpThemeVersion(this)
        recreate()
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_themes)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.themesRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.isNestedScrollingEnabled = false
        recycler.adapter = ThemeAdapter(AppPrefs.getAllThemeKeys(), AppPrefs.getEffectiveThemeKey(this)) { key ->
            AppPrefs.setSelectedTheme(this, key)
            applyChange()
        }

        setupAutoMode()
        buildRoleRows()
        setupShapeAndText()
        findViewById<Button>(R.id.resetButton).setOnClickListener { confirmReset() }
    }

    private fun setupAutoMode() {
        val autoSwitch = findViewById<SwitchCompat>(R.id.autoModeSwitch)
        val summary = findViewById<TextView>(R.id.autoModeSummary)
        autoSwitch.isChecked = AppPrefs.isAutoMode(this)
        summary.text = if (AppPrefs.isAutoMode(this)) {
            "Dark: ${AppPrefs.getThemeDisplayName(AppPrefs.getAutoDarkTheme(this))}  •  " +
                    "Light: ${AppPrefs.getThemeDisplayName(AppPrefs.getAutoLightTheme(this))}. " +
                    "Tap one dark and one light theme above to choose each."
        } else {
            "Off — the theme you tap above is always used."
        }
        autoSwitch.setOnCheckedChangeListener { _, checked ->
            AppPrefs.setAutoMode(this, checked)
            applyChange()
        }
    }

    private fun currentColor(role: ColorRole): Int = resolveColor(
        when (role) {
            ColorRole.ACCENT -> R.attr.appColorPrimary
            ColorRole.BUTTON -> R.attr.appColorButton
            ColorRole.BADGE -> R.attr.appColorBadge
            ColorRole.DELETE -> R.attr.appColorDelete
            ColorRole.BACKGROUND -> R.attr.appColorBackground
            ColorRole.SURFACE -> R.attr.appColorSurface
            ColorRole.TEXT -> R.attr.appColorOnSurface
        }
    )

    private fun buildRoleRows() {
        val container = findViewById<LinearLayout>(R.id.roleContainer)
        container.removeAllViews()
        ThemePalettes.UI_ORDER.forEachIndexed { index, role ->
            val row = layoutInflater.inflate(R.layout.item_color_role, container, false)
            val choice = AppPrefs.getRoleChoice(this, role)
            row.findViewById<TextView>(R.id.roleLabelText).text = role.label
            row.findViewById<TextView>(R.id.roleValueText).text =
                ThemePalettes.forRole(role).find { it.key == choice }?.name ?: "Theme default"
            (row.findViewById<View>(R.id.roleSwatch).background.mutate() as? GradientDrawable)?.setColor(currentColor(role))
            row.findViewById<View>(R.id.roleDivider).visibility =
                if (index == ThemePalettes.UI_ORDER.lastIndex) View.GONE else View.VISIBLE
            row.findViewById<View>(R.id.roleRow).setOnClickListener { showPalette(role) }
            container.addView(row)
        }
    }

    private fun showPalette(role: ColorRole) {
        val palette = ThemePalettes.forRole(role)
        val current = AppPrefs.getRoleChoice(this, role)
        val ringColor = resolveColor(R.attr.appColorOnSurface)

        val grid = GridLayout(this)
        grid.columnCount = 4
        grid.setPadding(dp(12), dp(8), dp(12), dp(8))
        val scroll = ScrollView(this)
        scroll.addView(grid)

        var dialogRef: AlertDialog? = null
        for (color in palette) {
            val selected = color.key == current
            val circle = View(this)
            circle.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            circle.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(color.hex))
                setStroke(dp(if (selected) 3 else 1), if (selected) ringColor else 0x44888888)
            }
            val label = TextView(this)
            label.text = color.name
            label.textSize = 11f
            label.gravity = Gravity.CENTER
            label.setTextColor(ringColor)

            val cell = LinearLayout(this)
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER
            cell.setPadding(dp(4), dp(8), dp(4), dp(8))
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            cell.layoutParams = params
            cell.addView(circle)
            cell.addView(label)
            cell.setOnClickListener {
                dialogRef?.dismiss()
                AppPrefs.setRoleChoice(this, role, color.key)
                applyChange()
            }
            grid.addView(cell)
        }

        dialogRef = AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(role.label)
            .setView(scroll)
            .setNeutralButton("Theme default") { _, _ ->
                AppPrefs.setRoleChoice(this, role, null)
                applyChange()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupShapeAndText() {
        val cornerKeys = listOf(AppPrefs.CORNERS_SHARP, AppPrefs.CORNERS_SOFT, AppPrefs.CORNERS_ROUND)
        val cornerLabels = arrayOf("Sharp", "Soft", "Round")
        findViewById<TextView>(R.id.cornersValueText).text =
            cornerLabels[cornerKeys.indexOf(AppPrefs.getCorners(this)).coerceAtLeast(0)]
        findViewById<View>(R.id.cornersRow).setOnClickListener {
            showChoice("Corner style", cornerLabels, cornerKeys.indexOf(AppPrefs.getCorners(this)).coerceAtLeast(0)) { which ->
                AppPrefs.setCorners(this, cornerKeys[which])
                applyChange()
            }
        }

        val sizeKeys = AppPrefs.TEXT_SCALE_KEYS
        val sizeLabels = sizeKeys.map { AppPrefs.textScaleLabel(it) }.toTypedArray()
        findViewById<TextView>(R.id.textSizeValueText).text = AppPrefs.textScaleLabel(AppPrefs.getTextScaleKey(this))
        findViewById<View>(R.id.textSizeRow).setOnClickListener {
            showChoice("Text size", sizeLabels, sizeKeys.indexOf(AppPrefs.getTextScaleKey(this)).coerceAtLeast(0)) { which ->
                AppPrefs.setTextScaleKey(this, sizeKeys[which])
                applyChange()
            }
        }

        val compactSwitch = findViewById<SwitchCompat>(R.id.compactSwitch)
        compactSwitch.isChecked = AppPrefs.isCompact(this)
        compactSwitch.setOnCheckedChangeListener { _, checked -> AppPrefs.setCompact(this, checked) }

        val animationsSwitch = findViewById<SwitchCompat>(R.id.animationsSwitch)
        animationsSwitch.isChecked = AppPrefs.isAnimationsEnabled(this)
        animationsSwitch.setOnCheckedChangeListener { _, checked -> AppPrefs.setAnimationsEnabled(this, checked) }
    }

    private fun showChoice(title: String, labels: Array<String>, selected: Int, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle(title)
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                dialog.dismiss()
                onPick(which)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Reset customizations")
            .setMessage("Clears every custom colour, the corner style, text size and compact cards. Your base theme stays.")
            .setPositiveButton("Reset") { _, _ ->
                AppPrefs.resetCustomizations(this)
                applyChange()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}