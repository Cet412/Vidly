package com.vidly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.vidly.app.data.api.ApiClient
import com.vidly.app.data.repository.DownloadRepository
import com.vidly.app.ui.theme.VidlyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val PREFS_NAME = "vidly_shared_url"
    private val downloadRepository = DownloadRepository()
    private val appContext = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cek SharedPreferences untuk URL yang dibagikan dari ShareReceiver
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val shareUrlFromPrefs = sharedPrefs.getString("url", null)

        // Handle share intent jika aplikasi dibuka dari share menu
        val shareUrl: String?
        if (intent?.action == "android.intent.action.SEND" && intent?.hasExtra(Intent.EXTRA_TEXT)!!) {
            shareUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            savedInstanceState?.putString("share_url", shareUrl)
        } else {
            shareUrl = savedInstanceState?.getString("share_url")
        }

        val finalUrl = shareUrl ?: shareUrlFromPrefs

        setContent {
            VidlyTheme {
                VidlyHome(shareUrl = finalUrl, downloadRepository = downloadRepository, appContext = appContext)
            }
        }
    }
}

/**
 * Menerima URL dari share intent sebagai parameter
 */
@Composable
fun VidlyHome(shareUrl: String? = null, downloadRepository: DownloadRepository, appContext: android.content.Context) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(shareUrl ?: "") }
    var status by rememberSaveable { mutableStateOf("Siap") }
    var loading by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Vidly")
            Text(text = "Unduh video dari URL yang dibagikan ke aplikasi.")

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL video") },
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        status = "Memeriksa backend..."
                        try {
                            val response = ApiClient.api.health()
                            if (response.isSuccessful && response.body()?.status == "ok") {
                                status = "Backend staging terhubung - Memulai unduhan..."
                                // Jalankan download ke MediaStore
                                val savedUri = downloadRepository.downloadVideo(appContext, url)
                                if (savedUri != null) {
                                    status = "Unduhan selesai! Disimpan di:\n${savedUri.toString()}"
                                } else {
                                    status = "Unduhan gagal - Cek log"
                                }
                            } else {
                                status = "Backend mengembalikan status ${response.code()}"
                            }
                        } catch (e: Exception) {
                            status = "Backend tidak dapat dihubungi"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Text("Cek Backend")
                }
            }

            Text(text = status)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VidlyHomePreview() {
    // Preview tidak memerlukan context real
    // Gunakan context dari preview environment
}