package com.vidly.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.vidly.app.data.manager.DownloadManager
import com.vidly.app.ui.download.DownloadQueueInline
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
import com.vidly.app.data.repository.DownloadRepository
import com.vidly.app.data.repository.DownloadResult
import com.vidly.app.ui.history.HistoryScreen
import com.vidly.app.data.repository.HistoryRepository
import com.vidly.app.service.DownloadService
import com.vidly.app.ui.theme.VidlyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingUrl: String? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "izin notifikasi: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()

        val shareUrl = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }

        if (shareUrl != null) {
            // Auto-download di background, langsung close activity
            val serviceIntent = Intent(this, DownloadService::class.java).apply {
                putExtra(DownloadService.EXTRA_URL, shareUrl)
            }
            startService(serviceIntent)
            finish()
            return
        }

        // Normal flow: buka UI
        pendingUrl = resolveUrl(savedInstanceState ?: Bundle())
        setContent {
            VidlyTheme {
                VidlyHome(
                    shareUrl = pendingUrl,
                    downloadRepository = DownloadRepository(),
                    historyRepository = HistoryRepository(applicationContext),
                    appContext = applicationContext
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /** URL dari intent share, lalu dari SharedPreferences yang diisi ShareReceiver. */
    private fun resolveUrl(savedInstanceState: Bundle): String? {
        val fromIntent = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val fromPrefs = getSharedPreferences(ShareReceiverPrefs, MODE_PRIVATE)
            .let { prefs ->
                prefs.getString("url", null)?.also {
                    prefs.edit().remove("url").apply() // konsumsi sekali pakai
                }
            }

        val extras = intent?.getStringExtra(EXTRA_URL)?.trim()?.takeIf { it.isNotEmpty() }

        val resolved = fromIntent ?: extras ?: fromPrefs ?: savedInstanceState.getString(EXTRA_URL)
        Log.d(TAG, "URL terpilih: ${resolved?.take(80)}")
        return resolved
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingUrl?.let { outState.putString(EXTRA_URL, it) }
    }

    companion object {
        const val TAG = "VidlyMain"
        const val EXTRA_URL = "share_url"
        const val ShareReceiverPrefs = "vidly_shared_url"
    }
}

@Composable
fun VidlyHome(
    shareUrl: String? = null,
    downloadRepository: DownloadRepository,
    historyRepository: HistoryRepository,
    appContext: android.content.Context
) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(shareUrl ?: "") }
    var loading by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val downloadManager = remember { DownloadManager.getInstance() }

    if (showHistory) {
        HistoryScreen(
            historyRepository = historyRepository,
            downloadRepository = downloadRepository,
            onBack = { showHistory = false }
        )
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Vidly", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { showHistory = true }) {
                    Text("Riwayat")
                }
            }
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
                    if (url.isBlank()) {
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        when (val result = downloadRepository.downloadVideo(appContext, url.trim())) {
                            is DownloadResult.Success -> {}
                            is DownloadResult.Failure -> {}
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator() else Text("Unduh")
            }

            // Download queue (always visible, inline)
            DownloadQueueInline(downloadManager = downloadManager)
        }
    }
}
