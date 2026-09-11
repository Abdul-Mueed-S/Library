package com.evu.library

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(AppPrefs.getThemeStyleResId(AppPrefs.getSelectedTheme(this)))
        super.onCreate(savedInstanceState)
    }
}