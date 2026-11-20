package com.vidly.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.vidly.app.BuildConfig
import com.vidly.app.data.api.ApiClient
import com.vidly.app.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed interface DownloadResult {
    data class Success(val uri: Uri, val bytes: Long) : DownloadResult
    data class Failure(val reason: String) : DownloadResult
}

class DownloadRepository {

    suspend fun downloadVideo(context: Context, url: String): DownloadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "mulai download url=$url")
        val notif = NotificationHelper(context)
        try {
            notif.showProgressNotification("Mengunduh...", 0)
            fetch(context, url, notif)
        } catch (e: Exception) {
            Log.e(TAG, "gagal download", e)
            notif.showErrorNotification("Unduhan gagal", e.message.orEmpty().take(100))
            DownloadResult.Failure("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun fetch(context: Context, url: String, notif: NotificationHelper): DownloadResult {
        val endpoint = "${BuildConfig.API_BASE_URL.trimEnd('/')}/api/v1/download"
        val payload = JSONObject().put("url", url).toString()
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d(TAG, "POST $endpoint")
        ApiClient.httpClient.newCall(request).execute().use { response ->
            Log.d(TAG, "response code=${response.code} contentType=${response.header("Content-Type")} length=${response.header("Content-Length")}")

            if (!response.isSuccessful) {
                val body = response.body?.string()?.take(300).orEmpty()
                Log.w(TAG, "backend menolak: ${response.code} $body")
                notif.showErrorNotification("Backend error", "${response.code}: $body")
                return DownloadResult.Failure("Backend ${response.code}: $body")
            }

            val body = response.body
            if (body == null) {
                Log.e(TAG, "response body null")
                notif.showErrorNotification("Error", "Response kosong")
                return DownloadResult.Failure("Response kosong dari backend")
            }

            val displayName = fileName(response.header("Content-Disposition"))
            val mimeType = guessMimeType(displayName, response.header("Content-Type"))
            val isImage = mimeType.startsWith("image/")
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            Log.d(TAG, "nama file=$displayName size=$contentLength mime=$mimeType isImage=$isImage")

            val uri = createMediaStoreEntry(context, displayName, mimeType, isImage)
                ?: run {
                    notif.showErrorNotification("Error", "Gagal membuat entri MediaStore")
                    return DownloadResult.Failure("Gagal membuat entri MediaStore")
                }

            val written = runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { out ->
                    var totalWritten = 0L
                    val buffer = ByteArray(8192)
                    val input = body.byteStream()
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        val progressPercent = (totalWritten * 100 / maxOf(contentLength, 1L)).toInt()
                        notif.showProgressNotification("Mengunduh: ${displayName.take(30)}", progressPercent)
                    }
                    totalWritten
                }
            }.getOrElse { e ->
                Log.e(TAG, "gagal menulis ke MediaStore, hapus entri", e)
                context.contentResolver.delete(uri, null, null)
                notif.showErrorNotification("Error", "Gagal menyimpan: ${e.message}")
                return DownloadResult.Failure("Gagal menyimpan file: ${e.message}")
            }

            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
            Log.d(TAG, "selesai: $written byte -> $uri")
            notif.showCompletionNotification("Unduhan selesai!", displayName)
            notif.cancelNotification()
            return DownloadResult.Success(uri, written)
        }
    }

    private fun createMediaStoreEntry(context: Context, displayName: String, mimeType: String, isImage: Boolean): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val baseDir = if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/Vidly")
            }
        }

        val collection = if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val uri = context.contentResolver.insert(collection, values)
        if (uri == null) {
            Log.e(TAG, "insert MediaStore mengembalikan null")
        }
        return uri
    }

    private fun fileName(contentDisposition: String?): String {
        val fromHeader = contentDisposition
            ?.substringAfter("filename=\"", "")
            ?.substringBefore("\"")
            ?.takeIf { it.isNotBlank() }
        return if (fromHeader != null) "Vidly_$fromHeader" else "Vidly_${System.currentTimeMillis()}.mp4"
    }

    private fun guessMimeType(filename: String, contentType: String?): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            else -> contentType?.substringBefore(';')?.trim() ?: "application/octet-stream"
        }
    }

    companion object {
        private const val TAG = "VidlyDownload"
    }
}

