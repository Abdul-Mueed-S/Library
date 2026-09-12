package com.evu.library

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val numberedSwitch = findViewById<SwitchCompat>(R.id.numberedListSwitch)
        numberedSwitch.isChecked = AppPrefs.isShowNumbers(this)
        numberedSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppPrefs.setShowNumbers(this, isChecked)
        }

        updateCurrentThemeLabel()

        findViewById<android.widget.LinearLayout>(R.id.themesRow).setOnClickListener {
            startActivity(Intent(this, ThemesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentThemeLabel()
    }

    private fun updateCurrentThemeLabel() {
        val current = AppPrefs.getThemeDisplayName(AppPrefs.getSelectedTheme(this))
        findViewById<android.widget.TextView>(R.id.currentThemeText).text = current
    }
}