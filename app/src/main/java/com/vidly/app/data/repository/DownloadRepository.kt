package com.vidly.app.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vidly.app.BuildConfig
import com.vidly.app.data.api.ApiClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Repository yang menangani alur download:
 * 1. Mengirim URL ke backend
 * 2. Menerima streaming file dari backend
 * 3. Menyimpan ke MediaStore Download/Vidly
 */
class DownloadRepository {

    /**
     * Memulai download video dari URL
     * @param context Context untuk akses file dan MediaStore
     * @param url URL video dari TikTok/Instagram
     * @return Uri ke file yang disimpan, atau null jika gagal
     */
    suspend fun downloadVideo(context: android.content.Context, url: String): Uri? {
        return try {
            // Health check backend
            val apiResponse = ApiClient.api.health()
            if (!apiResponse.isSuccessful || apiResponse.body()?.status != "ok") {
                null
            } else {
                // Persiapan directory penyimpanan
                val downloadsDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Vidly"
                )
                downloadsDir.mkdirs()

                // Buat nama file unik
                val targetFile = File(downloadsDir, "vidly_${System.currentTimeMillis()}.mp4")

                // Buat request ke backend download endpoint
                val json = """{"url":"$url"}"""
                val body = RequestBody.create("application/json".toMediaType(), json)

                val request = okhttp3.Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}/api/v1/download")
                    .post(body)
                    .build()

                // Eksekusi request dengan streaming
                val client = OkHttpClient()
                val response: okhttp3.Response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    null
                } else {
                    // Tulis streaming langsung ke file
                    FileOutputStream(targetFile).use { outputStream ->
                        response.body?.source().use { source ->
                            source?.buffer()?.use { buffer ->
                                outputStream.write(buffer.readByteArray())
                            }
                        }
                    }

                    // Simpan ke MediaStore
                    val savedUri = saveToMediaStore(context, targetFile)

                    // Kembalikan Uri ke file yang disimpan
                    savedUri
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("Range")
    private fun saveToMediaStore(context: android.content.Context, file: File): Uri {
        val values = ContentValues().apply {
            put("_display_name", file.name)
            put("mime_type", "video/mp4")
            put("relative_path", "${Environment.DIRECTORY_DOWNLOADS}/Vidly")
            put("size", file.length())
        }

        // Android 10 (API 29) dan atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.insert(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        } else {
            // Untuk versi Android lama
            return context.contentResolver.insert(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        }
    }
}