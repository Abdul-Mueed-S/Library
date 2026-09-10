package com.evu.library

sealed class ChipItem {
    object All : ChipItem()
    object Favourites : ChipItem()
    data class CategoryChip(val category: Category) : ChipItem()
    object AddNew : ChipItem()
}