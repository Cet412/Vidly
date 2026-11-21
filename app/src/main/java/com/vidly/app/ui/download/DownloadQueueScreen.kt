package com.vidly.app.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidly.app.data.manager.DownloadManager
import com.vidly.app.data.model.DownloadStatus
import com.vidly.app.data.model.progressText
import com.vidly.app.data.model.sizeText

@Composable
fun DownloadQueueSheet(
    downloadManager: DownloadManager = DownloadManager.getInstance(),
    onDismiss: () -> Unit = {}
) {
    val downloads by downloadManager.downloads.collectAsState()

    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada download", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Antrian Unduhan (${downloads.size})", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDismiss, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(downloads, key = { it.id }) { item ->
                DownloadQueueItem(
                    item = item,
                    onCancel = { downloadManager.cancelDownload(item.id) },
                    onPause = { downloadManager.pauseDownload(item.id) },
                    onRemove = { downloadManager.removeDownload(item.id) }
                )
            }
        }
    }
}

@Composable
fun DownloadQueueItem(
    item: com.vidly.app.data.model.DownloadItem,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.take(40),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${item.progressText()} • ${item.sizeText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onRemove, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.padding(4.dp))
                    }
                }
            }

            if (item.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            if (item.errorMessage != null) {
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
