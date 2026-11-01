package com.evu.library

class DraftsActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_drafts
    override val recyclerViewId = R.id.draftsRecyclerView

    override suspend fun loadBooks() = db.bookDao().getDraftBooks()

    override fun dialogTitle(book: Book) =
        if (book.flaggedDuplicate) "${book.title} (Also a Duplicate)" else book.title

    override fun buildActions(book: Book) = listOf(
        actions.edit(book), actions.delete(book), actions.publish(book)
    )
}