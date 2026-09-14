package com.evu.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ImportExportActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private var pendingExportContent: String = ""
    private var pendingFullBackupContent: String = ""

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { it.write(pendingExportContent.toByteArray()) }
            Toast.makeText(this, "Export saved", Toast.LENGTH_SHORT).show()
        }
    }

    private val createFullBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { it.write(pendingFullBackupContent.toByteArray()) }
            Toast.makeText(this, "Full backup saved", Toast.LENGTH_SHORT).show()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleImportFile(uri)
    }

    private val openFullBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleFullBackupFile(uri)
    }

    private val openVaultMetadataLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleVaultMetadataFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_export)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        db = AppDatabase.getDatabase(this)

        val mode = intent.getStringExtra("mode")
        val exportSection = findViewById<android.widget.LinearLayout>(R.id.exportSection)
        val importSection = findViewById<android.widget.LinearLayout>(R.id.importSection)
        val fullBackupSection = findViewById<android.widget.LinearLayout>(R.id.fullBackupSection)
        val pageTitle = findViewById<android.widget.TextView>(R.id.pageTitle)

        when (mode) {
            "export" -> {
                importSection.visibility = android.view.View.GONE
                fullBackupSection.visibility = android.view.View.GONE
                pageTitle.text = "Export"
            }
            "import" -> {
                exportSection.visibility = android.view.View.GONE
                fullBackupSection.visibility = android.view.View.GONE
                pageTitle.text = "Import"
            }
            else -> pageTitle.text = "Export / Import"
        }

        findViewById<android.widget.Button>(R.id.exportButton).setOnClickListener { startExport() }
        findViewById<android.widget.Button>(R.id.importButton).setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/csv", "text/*", "*/*"))
        }
        findViewById<android.widget.Button>(R.id.fullBackupButton).setOnClickListener { startFullBackup() }
        findViewById<android.widget.Button>(R.id.fullRestoreButton).setOnClickListener { confirmFullRestore() }
        findViewById<android.widget.Button>(R.id.restoreVaultMetadataButton).setOnClickListener {
            openVaultMetadataLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        }
    }

    private fun startExport() {
        val isJson = findViewById<RadioGroup>(R.id.formatGroup).checkedRadioButtonId == R.id.formatJson
        val includeCategories = findViewById<CheckBox>(R.id.includeCategoriesCheckbox).isChecked
        val includeFavorites = findViewById<CheckBox>(R.id.includeFavoritesCheckbox).isChecked
        val includeDuplicateFlags = findViewById<CheckBox>(R.id.includeDuplicateFlagsCheckbox).isChecked

        lifecycleScope.launch {
            val books = db.bookDao().getAllBooks()
            val categories = db.categoryDao().getAllCategories().associate { it.id to it.name }
            pendingExportContent = if (isJson) {
                ExportImportUtils.booksToJson(books, categories, includeCategories, includeFavorites, includeDuplicateFlags)
            } else {
                ExportImportUtils.booksToCsv(books, categories, includeCategories, includeFavorites, includeDuplicateFlags)
            }
            val extension = if (isJson) "json" else "csv"
            val count = books.size
            createDocumentLauncher.launch("libraryData_export_${count}_books.$extension")
        }
    }

    private fun startFullBackup() {
        lifecycleScope.launch {
            val books = db.bookDao().getAllBooks()
            val categories = db.categoryDao().getAllCategories()
            pendingFullBackupContent = ExportImportUtils.fullBackupToJson(categories, books)
            createFullBackupLauncher.launch("library_full_backup_${books.size}_books.json")
        }
    }

    private fun confirmFullRestore() {
        AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setTitle("Restore Full Backup")
            .setMessage("This will DELETE your current library and replace it entirely with the backup file's contents. This cannot be undone. Continue?")
            .setPositiveButton("Choose Backup File") { _, _ ->
                openFullBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleFullBackupFile(uri: Uri) {
        val content = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        val data = ExportImportUtils.parseFullBackupJson(content)
        if (data == null) {
            Toast.makeText(this, "Invalid or corrupted backup file", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            db.bookDao().deleteAllBooks()
            db.categoryDao().deleteAllCategories()
            for (cat in data.categories) {
                db.categoryDao().insertCategoryWithId(Category(id = cat.id, name = cat.name))
            }
            for (book in data.books) {
                db.bookDao().insertBookWithId(
                    Book(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        edition = book.edition,
                        year = book.year,
                        isbn = book.isbn,
                        isFavorite = book.isFavorite,
                        categoryId = book.categoryId,
                        flaggedDuplicate = book.flaggedDuplicate,
                        isDraft = book.isDraft
                    )
                )
            }
            Toast.makeText(this@ImportExportActivity, "Library restored: ${data.books.size} books, ${data.categories.size} categories", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVaultMetadataFile(uri: Uri) {
        val content = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        if (content.isBlank()) {
            Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val count = VaultManager.restoreVaultMetadataJson(this, content)
            Toast.makeText(this, "Restored $count librar${if (count == 1) "y" else "ies"} (name/location only — restore each library's books separately)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid vault metadata file", Toast.LENGTH_SHORT).show()
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
            ImportHolder.defaultIncludeFavorites =
                findViewById<android.widget.CheckBox>(R.id.importFavoritesDefaultCheckbox)?.isChecked ?: true
            ImportHolder.defaultIncludeDuplicateFlags =
                findViewById<android.widget.CheckBox>(R.id.importDuplicateFlagsDefaultCheckbox)?.isChecked ?: false
            startActivity(Intent(this@ImportExportActivity, ImportPreviewActivity::class.java))
        }
    }
}