package com.evu.library

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OwnershipMatch(val book: Book, val libraryName: String, val isStrong: Boolean)

class OwnershipCheckActivity : BaseActivity() {

    private lateinit var adapter: OwnershipAdapter
    private lateinit var banner: TextView
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ownership_check)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        banner = findViewById(R.id.resultBanner)

        adapter = OwnershipAdapter(emptyList())
        val recycler = findViewById<RecyclerView>(R.id.resultsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val input = findViewById<EditText>(R.id.queryInput)
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(200)
                    search(s.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        input.requestFocus()
        showBanner("Searches all your libraries.", neutral = true)
    }

    private fun attrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun showBanner(text: String, neutral: Boolean = false, good: Boolean = false) {
        banner.text = text
        banner.setTextColor(
            when {
                neutral -> attrColor(R.attr.appColorOnSurface)
                good -> attrColor(R.attr.appColorPrimary)
                else -> attrColor(R.attr.appColorDelete)
            }
        )
        banner.alpha = if (neutral) 0.7f else 1f
    }

    private suspend fun search(raw: String) {
        val query = Utils.clean(raw)
        if (query.length < 2) {
            adapter.update(emptyList())
            showBanner("Searches all your libraries.", neutral = true)
            return
        }
        val queryIsbn = Utils.formatIsbn(query)
        val isbnMode = queryIsbn.length >= 10 && query.none { it.isLetter() }

        val matches = mutableListOf<OwnershipMatch>()
        for (vault in VaultManager.getAllVaults(this)) {
            val books = AppDatabase.getDatabaseForVault(this, vault).bookDao().getAllBooks()
            for (book in books) {
                if (isbnMode) {
                    if (!book.isbn.isNullOrBlank() && Utils.formatIsbn(book.isbn) == queryIsbn) {
                        matches.add(OwnershipMatch(book, vault.name, true))
                    }
                } else {
                    val strong = Utils.clean(book.title).equals(query, ignoreCase = true)
                    val similar = book.title.contains(query, ignoreCase = true) ||
                            book.author?.contains(query, ignoreCase = true) == true
                    if (strong || similar) matches.add(OwnershipMatch(book, vault.name, strong))
                }
            }
        }
        val sorted = matches.sortedWith(compareByDescending<OwnershipMatch> { it.isStrong }.thenBy { it.book.title.lowercase() })
        adapter.update(sorted)

        val strongMatches = sorted.filter { it.isStrong }
        when {
            strongMatches.isNotEmpty() -> {
                val copies = strongMatches.sumOf { it.book.copies }
                val libraries = strongMatches.map { it.libraryName }.distinct()
                showBanner(
                    "✓ Yes — you own this ($copies cop${if (copies == 1) "y" else "ies"} in ${libraries.joinToString(", ")})",
                    good = true
                )
            }
            sorted.isNotEmpty() -> showBanner("Maybe — similar entries found below", neutral = true)
            else -> showBanner("✗ Not in any library")
        }
    }
}

class OwnershipAdapter(
    private var items: List<OwnershipMatch>
) : RecyclerView.Adapter<OwnershipAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.ownTitleText)
        val subtitle: TextView = view.findViewById(R.id.ownSubtitleText)
        val library: TextView = view.findViewById(R.id.ownLibraryText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_ownership_result, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val match = items[position]
        val book = match.book
        holder.title.text = book.title + (if (book.copies > 1) "  ×${book.copies}" else "") + (if (book.isDraft) " (Draft)" else "")
        holder.subtitle.text = listOfNotNull(
            book.author?.takeIf { it.isNotBlank() },
            book.edition?.takeIf { it.isNotBlank() }?.let { "Ed. $it" },
            book.year?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ").ifEmpty { "—" }
        holder.library.text = "In: ${match.libraryName}  •  " + if (match.isStrong) "Exact match" else "Similar"
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<OwnershipMatch>) {
        items = newItems
        notifyDataSetChanged()
    }
}