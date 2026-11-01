package com.evu.library

class FavouritesActivity : BookListActivity() {
    override val layoutRes = R.layout.activity_favourites
    override val recyclerViewId = R.id.favouritesRecyclerView

    override suspend fun loadBooks() =
        db.bookDao().getFavoriteBooks().filter { !it.isDraft && !it.flaggedDuplicate }

    override fun buildActions(book: Book) = listOf(
        actions.edit(book), actions.delete(book), actions.removeFromFavourites(book),
        actions.draft(book), actions.lend(book)
    )
}