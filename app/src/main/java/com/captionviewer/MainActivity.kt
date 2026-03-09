package com.captionviewer

import android.app.Activity
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.TextView

/**
 * Full-screen, landscape activity for Android TV.
 *
 * - Starts a local HTTP server ([CaptionServer]) when the activity is visible.
 * - Displays received captions in large white text near the bottom of the screen.
 * - Automatically hides the caption after [CAPTION_DISPLAY_MS] milliseconds.
 * - Shows the device IP address and port so operators know where to send captions.
 */
class MainActivity : Activity() {

    private lateinit var captionText: TextView
    private lateinit var statusText: TextView
    private lateinit var server: CaptionServer

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideCaptionRunnable = Runnable { hideCaption() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captionText = findViewById(R.id.caption_text)
        statusText = findViewById(R.id.status_text)

        server = CaptionServer { text ->
            mainHandler.post { showCaption(text) }
        }
    }

    override fun onStart() {
        super.onStart()
        startServer()
        updateStatusText()
    }

    override fun onStop() {
        super.onStop()
        stopServer()
    }

    private fun startServer() {
        try {
            server.start()
            Log.i(TAG, "Caption server started on port ${CaptionServer.PORT}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
        }
    }

    private fun stopServer() {
        server.stop()
        Log.i(TAG, "Caption server stopped")
    }

    private fun showCaption(text: String) {
        mainHandler.removeCallbacks(hideCaptionRunnable)
        captionText.text = text
        captionText.visibility = View.VISIBLE
        mainHandler.postDelayed(hideCaptionRunnable, CAPTION_DISPLAY_MS)
    }

    private fun hideCaption() {
        captionText.visibility = View.INVISIBLE
    }

    private fun updateStatusText() {
        val ip = getLocalIpAddress()
        statusText.text = getString(R.string.status_listening, ip, CaptionServer.PORT)
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager =
                applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "device-ip"
        }
    }

    companion object {
        private const val TAG = "CaptionViewer"
        private const val CAPTION_DISPLAY_MS = 10_000L
    }
}
