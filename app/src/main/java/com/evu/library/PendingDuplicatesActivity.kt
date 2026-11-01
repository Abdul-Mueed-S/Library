package com.evu.library

class PendingDuplicatesActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_duplicates
    override val recyclerViewId = R.id.duplicatesRecyclerView
    override val showDuplicateTag = false

    override suspend fun loadBooks() = db.bookDao().getFlaggedDuplicates()

    override fun buildActions(book: Book) = listOf(
        actions.edit(book), actions.delete(book), actions.moveToSeries(book),
        actions.draft(book), actions.lend(book)
    )
}