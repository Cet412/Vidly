package com.vidly.app.data.repository

sealed interface DownloadError {
    val userMessage: String

    data class Network(val technical: String) : DownloadError {
        override val userMessage: String = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
    }

    data class Timeout(val technical: String) : DownloadError {
        override val userMessage: String = "Koneksi timeout, coba lagi."
    }

    data class Backend(val code: Int, val body: String) : DownloadError {
        override val userMessage: String = when (code) {
            400 -> "URL tidak valid atau platform belum didukung."
            404 -> "Video tidak ditemukan."
            429 -> "Batas unduhan tercapai, coba lagi dalam 1 jam."
            500 -> "Server sedang sibuk, coba lagi nanti."
            502 -> "Server sedang sibuk, coba lagi nanti."
            503 -> "Server sedang maintenance, coba lagi nanti."
            else -> "Gagal dari server (kode $code)."
        }
    }

    data class RateLimited(val retryAfterSeconds: Int) : DownloadError {
        override val userMessage: String =
            "Batas unduhan tercapai. Coba lagi dalam ${retryAfterSeconds / 60} menit."
    }

    data class Storage(val technical: String) : DownloadError {
        override val userMessage: String = when {
            technical.contains("permission", ignoreCase = true) -> "Izin penyimpanan ditolak."
            technical.contains("space", ignoreCase = true) -> "Penyimpanan penuh."
            technical.contains("no space", ignoreCase = true) -> "Penyimpanan penuh."
            else -> "Gagal menyimpan file: $technical"
        }
    }

    data class Unknown(val technical: String) : DownloadError {
        override val userMessage: String = "Terjadi kesalahan: $technical"
    }
}
