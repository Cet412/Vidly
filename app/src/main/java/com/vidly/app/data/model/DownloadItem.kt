package com.vidly.app.data.model

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val status: DownloadStatus,
    val progress: Int = 0,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}

fun DownloadItem.progressPercent(): Int {
    return if (totalSize > 0) ((downloadedSize * 100) / totalSize).toInt() else 0
}

fun DownloadItem.progressText(): String {
    return when (status) {
        DownloadStatus.QUEUED -> "Antri"
        DownloadStatus.DOWNLOADING -> "${progressPercent()}%"
        DownloadStatus.COMPLETED -> "Selesai"
        DownloadStatus.FAILED -> "Gagal"
        DownloadStatus.PAUSED -> "Dijeda"
        DownloadStatus.CANCELLED -> "Dibatalkan"
    }
}

fun DownloadItem.sizeText(): String {
    return formatBytes(totalSize)
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
