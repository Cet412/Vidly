package com.vidly.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import android.content.SharedPreferences

class ShareReceiver : BroadcastReceiver() {
    private val TAG = "ShareReceiver"
    private val PREFS_NAME = "vidly_shared_url"

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (url != null && !url.isEmpty()) {
            Log.d(TAG, "URL diterima via share intent: $url")

            // Simpan URL ke shared preferences agar MainActivity bisa baca
            val sharedPref: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPref.edit().putString("url", url).apply()

            // Tampilkan toast
            Toast.makeText(context, "Link video diterima", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "Share intent terekstur tanpa URL")
        }
    }
}