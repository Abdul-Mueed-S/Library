package com.evu.library

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private var appliedThemeVersion = 0

    // Text-size setting: scales the system font size for this app only.
    override fun attachBaseContext(newBase: Context) {
        val scale = AppPrefs.getTextScale(newBase)
        if (scale == 1f) {
            super.attachBaseContext(newBase)
            return
        }
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale * scale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPrefs.applyTheme(this)
        appliedThemeVersion = AppPrefs.getThemeVersion(this)
        super.onCreate(savedInstanceState)
    }

    // Screens left open behind the Themes screen refresh themselves when you come back to them.
    override fun onResume() {
        super.onResume()
        if (AppPrefs.getThemeVersion(this) != appliedThemeVersion) recreate()
    }
}