package com.evu.library

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatalogueActivity : BaseActivity() {

    private val libraryChecks = mutableListOf<Pair<LibraryVault, CheckBox>>()
    private val columnChecks = LinkedHashMap<CatalogueColumn, CheckBox>()
    private lateinit var categorySpinner: Spinner
    private lateinit var seriesSpinner: Spinner
    private var pendingOptions: CatalogueOptions? = null

    private val createPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val options = pendingOptions
        if (uri != null && options != null) writePdf(uri, options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogue)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        categorySpinner = findViewById(R.id.categorySpinner)
        seriesSpinner = findViewById(R.id.seriesSpinner)

        // Libraries (the current one is ticked by default)
        val libraryContainer = findViewById<LinearLayout>(R.id.libraryContainer)
        val activeId = VaultManager.getActiveVault(this).id
        for (vault in VaultManager.getAllVaults(this)) {
            val check = CheckBox(this)
            check.text = vault.name
            check.isChecked = vault.id == activeId
            check.setOnCheckedChangeListener { _, _ -> refreshFilterChoices() }
            libraryContainer.addView(check)
            libraryChecks.add(vault to check)
        }

        // Columns
        val columnContainer = findViewById<LinearLayout>(R.id.columnContainer)
        for (column in CatalogueColumn.values()) {
            val check = CheckBox(this)
            check.text = column.label.let { if (column == CatalogueColumn.COPIES) "Copies (Qty)" else it }
            check.isChecked = true
            columnContainer.addView(check)
            columnChecks[column] = check
        }

        findViewById<Spinner>(R.id.groupSpinner).adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("No grouping", "Group by category", "Group by series")
        )
        findViewById<Spinner>(R.id.sortSpinner).adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Title A–Z", "Author A–Z", "Newest first")
        )

        findViewById<Button>(R.id.createCatalogueButton).setOnClickListener { startCreate() }
        refreshFilterChoices()
    }

    private fun fillSpinner(spinner: Spinner, items: List<String>, keep: String?) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        val index = if (keep != null) items.indexOf(keep) else -1
        spinner.setSelection(if (index >= 0) index else 0)
    }

    // Category and series choices = every name found in the ticked libraries.
    private fun refreshFilterChoices() {
        if (!::categorySpinner.isInitialized) return
        val keepCategory = categorySpinner.selectedItem as? String
        val keepSeries = seriesSpinner.selectedItem as? String
        lifecycleScope.launch {
            val categories = sortedSetOf<String>(String.CASE_INSENSITIVE_ORDER)
            val series = sortedSetOf<String>(String.CASE_INSENSITIVE_ORDER)
            for ((vault, check) in libraryChecks) {
                if (!check.isChecked) continue
                val db = AppDatabase.getDatabaseForVault(this@CatalogueActivity, vault)
                db.categoryDao().getAllCategories().forEach { categories.add(it.name) }
                db.seriesDao().getAllSeries().forEach { series.add(it.name) }
            }
            fillSpinner(categorySpinner, listOf("All categories", "No category") + categories, keepCategory)
            fillSpinner(seriesSpinner, listOf("All series", "Not in a series") + series, keepSeries)
        }
    }

    private fun spinnerFilter(spinner: Spinner): String? = when (spinner.selectedItemPosition) {
        0 -> null
        1 -> CatalogueGenerator.FILTER_NONE
        else -> spinner.selectedItem as String
    }

    private fun startCreate() {
        val ids = libraryChecks.filter { it.second.isChecked }.map { it.first.id }.toSet()
        if (ids.isEmpty()) {
            Toast.makeText(this, "Tick at least one library", Toast.LENGTH_SHORT).show()
            return
        }
        val columns = columnChecks.filter { it.value.isChecked }.keys
        pendingOptions = CatalogueOptions(
            libraryIds = ids,
            categoryFilter = spinnerFilter(categorySpinner),
            seriesFilter = spinnerFilter(seriesSpinner),
            includeDrafts = findViewById<CheckBox>(R.id.includeDraftsCheck).isChecked,
            includeDuplicates = findViewById<CheckBox>(R.id.includeDuplicatesCheck).isChecked,
            onlyFavourites = findViewById<CheckBox>(R.id.onlyFavouritesCheck).isChecked,
            onlyLent = findViewById<CheckBox>(R.id.onlyLentCheck).isChecked,
            groupBy = findViewById<Spinner>(R.id.groupSpinner).selectedItemPosition,
            sortBy = findViewById<Spinner>(R.id.sortSpinner).selectedItemPosition,
            columns = columns.toSet(),
            pagePerLibrary = findViewById<CheckBox>(R.id.pagePerLibraryCheck).isChecked
        )
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val label = if (ids.size == 1) {
            BackupFiles.safeVaultName(libraryChecks.first { it.first.id == ids.first() }.first.name)
        } else "${ids.size}_libraries"
        createPdfLauncher.launch("Library_catalogue_${label}_$date.pdf")
    }

    private fun writePdf(uri: Uri, options: CatalogueOptions) {
        lifecycleScope.launch {
            val libraries = mutableListOf<CatalogueLibrary>()
            for ((vault, _) in libraryChecks) {
                if (vault.id !in options.libraryIds) continue
                val db = AppDatabase.getDatabaseForVault(this@CatalogueActivity, vault)
                libraries.add(
                    CatalogueLibrary(
                        name = vault.name,
                        books = db.bookDao().getAllBooks(),
                        categoryNames = db.categoryDao().getAllCategories().associate { it.id to it.name },
                        series = db.seriesDao().getAllSeries().associateBy { it.id }
                    )
                )
            }
            val ok = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri)?.use {
                        CatalogueGenerator.write(it, libraries, options)
                        true
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(
                this@CatalogueActivity,
                if (ok) "Catalogue saved" else "Couldn't create the catalogue",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}