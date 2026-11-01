package com.evu.library

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SeriesDao {
    @Insert
    suspend fun insertSeries(series: Series): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSeriesWithId(list: List<Series>)

    @Query("DELETE FROM series")
    suspend fun deleteAllSeries()

    @Query("SELECT * FROM series ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllSeries(): List<Series>

    @Query("SELECT COUNT(*) FROM series")
    suspend fun getSeriesCount(): Int

    @Query("SELECT * FROM series WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Series?

    @Query("SELECT * FROM series WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Series?

    @Update
    suspend fun updateSeries(series: Series)

    @Delete
    suspend fun deleteSeries(series: Series)
}