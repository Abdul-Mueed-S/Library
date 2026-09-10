package com.evu.library

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class LibraryTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}