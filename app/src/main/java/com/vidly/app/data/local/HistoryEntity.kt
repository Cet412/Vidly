package com.vidly.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val platform: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: String = "success"
)
