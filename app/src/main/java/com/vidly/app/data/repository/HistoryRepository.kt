package com.vidly.app.data.repository

import android.content.Context
import com.vidly.app.data.local.HistoryDao
import com.vidly.app.data.local.HistoryEntity
import com.vidly.app.data.local.VidlyDatabase
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {
    private val dao: HistoryDao = VidlyDatabase.getInstance(context).historyDao()

    fun getAll(): Flow<List<HistoryEntity>> = dao.getAll()

    fun search(query: String): Flow<List<HistoryEntity>> = dao.search(query)

    fun filterByPlatform(platform: String): Flow<List<HistoryEntity>> = dao.filterByPlatform(platform)

    fun searchAndFilter(query: String, platform: String): Flow<List<HistoryEntity>> =
        dao.searchAndFilter(query, platform)

    suspend fun insert(item: HistoryEntity): Long = dao.insert(item)

    suspend fun delete(item: HistoryEntity) = dao.delete(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun count(): Int = dao.count()
}
