package com.evu.library

import android.widget.TextView

class LentOutActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_book_list
    override val recyclerViewId = R.id.bookRecyclerView

    override fun onSetup() {
        findViewById<TextView>(R.id.listTitleText).text = "Lent Out"
    }

    override suspend fun loadBooks() = db.bookDao().getLentBooks()

    override fun buildActions(book: Book) = listOf(
        actions.lend(book), actions.edit(book), actions.delete(book)
    )
}