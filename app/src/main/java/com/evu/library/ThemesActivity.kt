package com.evu.library

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ThemesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_themes)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.themesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ThemeAdapter(
            AppPrefs.getAllThemeKeys(),
            AppPrefs.getSelectedTheme(this)
        ) { selectedKey ->
            AppPrefs.setSelectedTheme(this, selectedKey)
            // Restart the whole task fresh so every activity in the back stack
            // (Main, Settings, etc.) picks up the new theme via BaseActivity.
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}