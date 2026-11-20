package com.vidly.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vidly.app.MainActivity

class ShareReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        Log.d(TAG, "share intent diterima, url=${url?.take(80)}")

        if (url.isNullOrEmpty()) {
            Log.w(TAG, "EXTRA_TEXT kosong, share diabaikan")
            return
        }

        // Simpan untuk dibaca MainActivity saat onCreate (dikonsumsi sekali pakai).
        context.getSharedPreferences(MainActivity.ShareReceiverPrefs, Context.MODE_PRIVATE)
            .edit()
            .putString("url", url)
            .commit()

        // Buka app supaya user melihat hasilnya, bukan hanya toast di latar.
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
    }

    companion object {
        private const val TAG = "ShareReceiver"
    }
}
