package com.vidly.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY downloadedAt DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE platform = :platform ORDER BY downloadedAt DESC")
    fun filterByPlatform(platform: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' AND platform = :platform ORDER BY downloadedAt DESC")
    fun searchAndFilter(query: String, platform: String): Flow<List<HistoryEntity>>

    @Delete
    suspend fun delete(item: HistoryEntity)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM download_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM download_history")
    suspend fun count(): Int
}
