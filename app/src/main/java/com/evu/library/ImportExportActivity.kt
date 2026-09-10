package com.evu.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ImportExportActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var pendingExportContent: String = ""

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { it.write(pendingExportContent.toByteArray()) }
            Toast.makeText(this, "Export saved", Toast.LENGTH_SHORT).show()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleImportFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_export)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)

        val mode = intent.getStringExtra("mode")
        val exportSection = findViewById<android.widget.LinearLayout>(R.id.exportSection)
        val importSection = findViewById<android.widget.LinearLayout>(R.id.importSection)
        val divider = findViewById<android.view.View>(R.id.divider)
        val pageTitle = findViewById<android.widget.TextView>(R.id.pageTitle)

        when (mode) {
            "export" -> {
                importSection.visibility = android.view.View.GONE
                divider.visibility = android.view.View.GONE
                pageTitle.text = "Export"
            }
            "import" -> {
                exportSection.visibility = android.view.View.GONE
                divider.visibility = android.view.View.GONE
                pageTitle.text = "Import"
            }
            else -> pageTitle.text = "Export / Import"
        }

        findViewById<android.widget.Button>(R.id.exportButton).setOnClickListener { startExport() }
        findViewById<android.widget.Button>(R.id.importButton).setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/csv", "text/*", "*/*"))
        }
    }

    private fun startExport() {
        val isJson = findViewById<RadioGroup>(R.id.formatGroup).checkedRadioButtonId == R.id.formatJson
        val includeCategories = findViewById<CheckBox>(R.id.includeCategoriesCheckbox).isChecked

        lifecycleScope.launch {
            val books = db.bookDao().getAllBooks()
            val categories = db.categoryDao().getAllCategories().associate { it.id to it.name }
            pendingExportContent = if (isJson) {
                ExportImportUtils.booksToJson(books, categories, includeCategories)
            } else {
                ExportImportUtils.booksToCsv(books, categories, includeCategories)
            }
            val extension = if (isJson) "json" else "csv"
            val count = books.size
            createDocumentLauncher.launch("libraryData_export_${count}_books.$extension")
        }
    }

    private fun handleImportFile(uri: Uri) {
        val content = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        if (content.isBlank()) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
            return
        }
        val isJson = content.trim().startsWith("[") || content.trim().startsWith("{")
        val parsed = try {
            if (isJson) ExportImportUtils.parseJson(content) else ExportImportUtils.parseCsv(content)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid file format", Toast.LENGTH_SHORT).show()
            return
        }
        if (parsed.isEmpty()) {
            Toast.makeText(this, "No valid entries found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val mutableParsed = parsed.toMutableList()
            val existing = db.bookDao().getAllBooks()
            ExportImportUtils.computeDuplicateFlags(mutableParsed, existing)
            ImportHolder.books = mutableParsed
            ImportHolder.defaultIncludeCategories =
                findViewById<android.widget.CheckBox>(R.id.importCategoriesDefaultCheckbox)?.isChecked ?: true
            startActivity(Intent(this@ImportExportActivity, ImportPreviewActivity::class.java))
        }
    }
}