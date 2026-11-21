package com.vidly.app.data.manager

import com.vidly.app.data.model.DownloadItem
import com.vidly.app.data.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DownloadManager {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun addDownload(url: String, title: String): String {
        val id = UUID.randomUUID().toString()
        val item = DownloadItem(
            id = id,
            url = url,
            title = title,
            status = DownloadStatus.QUEUED,
            progress = 0
        )
        _downloads.value = _downloads.value + item
        return id
    }

    fun updateProgress(downloadId: String, downloadedSize: Long, totalSize: Long) {
        val percent = if (totalSize > 0) ((downloadedSize * 100) / totalSize).toInt() else 0
        _downloads.value = _downloads.value.map { item ->
            if (item.id == downloadId) {
                item.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadedSize = downloadedSize,
                    totalSize = totalSize,
                    progress = percent
                )
            } else item
        }
    }

    fun markCompleted(downloadId: String) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == downloadId) {
                item.copy(status = DownloadStatus.COMPLETED, progress = 100)
            } else item
        }
    }

    fun markFailed(downloadId: String, error: String) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == downloadId) {
                item.copy(status = DownloadStatus.FAILED, errorMessage = error)
            } else item
        }
    }

    fun pauseDownload(downloadId: String) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == downloadId) {
                item.copy(status = DownloadStatus.PAUSED)
            } else item
        }
    }

    fun cancelDownload(downloadId: String) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == downloadId) {
                item.copy(status = DownloadStatus.CANCELLED)
            } else item
        }
    }

    fun removeDownload(downloadId: String) {
        _downloads.value = _downloads.value.filterNot { it.id == downloadId }
    }

    fun clearCompleted() {
        _downloads.value = _downloads.value.filterNot { it.status == DownloadStatus.COMPLETED }
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager().also { INSTANCE = it }
            }
        }
    }
}
