package com.watch.animekai

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var customView: View? = null
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private val defaultUrl = "https://animekai.to/home"
    private val cookieKey = "SavedCookies"
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)

        // Enable cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        Log.d(TAG, "Cookies enabled for WebView")

        // Restore cookies
        restoreCookies()

        // Configure WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                saveCookies()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                Log.d(TAG, "Entering fullscreen mode")
                customView = view
                customViewContainer?.visibility = View.VISIBLE
                customViewContainer?.addView(view)
                customViewCallback = callback
                webView.visibility = View.GONE

                // Allow free rotation in fullscreen mode
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            override fun onHideCustomView() {
                Log.d(TAG, "Exiting fullscreen mode")
                customViewContainer?.visibility = View.GONE
                customViewContainer?.removeView(customView)
                customView = null
                customViewCallback = null
                webView.visibility = View.VISIBLE

                // Reset to default orientation
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.saveFormData = true
        Log.d(TAG, "WebView settings configured")

        // Load the saved URL or default URL
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("webViewUrl", defaultUrl)
        Log.d(TAG, "Loading URL: ${savedUrl ?: defaultUrl}")
        webView.loadUrl(savedUrl ?: defaultUrl)

        // Long press for 5 seconds to change URL
        webView.setOnTouchListener(object : View.OnTouchListener {
            private val longPressHandler = Handler(Looper.getMainLooper())
            private var isLongPress = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isLongPress = true
                        longPressHandler.postDelayed({
                            if (isLongPress) {
                                Log.d(TAG, "Long press detected, showing URL change dialog")
                                showChangeUrlDialog(sharedPreferences, webView)
                            }
                        }, 5000)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isLongPress = false
                        longPressHandler.removeCallbacksAndMessages(null)
                    }
                }
                return false
            }
        })
    }

    private fun saveCookies() {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(defaultUrl)
        if (cookies != null) {
            Log.d(TAG, "Saving cookies: $cookies")
            val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString(cookieKey, cookies).apply()
            cookieManager.flush()
        } else {
            Log.d(TAG, "No cookies to save")
        }
    }

    private fun restoreCookies() {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val cookies = sharedPreferences.getString(cookieKey, null)
        if (cookies != null) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(defaultUrl, cookies)
            cookieManager.flush()
            Log.d(TAG, "Restored cookies: $cookies")
        } else {
            Log.d(TAG, "No cookies found to restore")
        }
    }

    private fun showChangeUrlDialog(sharedPreferences: android.content.SharedPreferences, webView: WebView) {
        val editText = EditText(this)
        editText.hint = "Enter new URL"
        val dialog = AlertDialog.Builder(this)
            .setTitle("Change URL")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = editText.text.toString()
                if (newUrl.isNotBlank()) {
                    sharedPreferences.edit().putString("webViewUrl", newUrl).apply()
                    webView.loadUrl(newUrl)
                    Log.d(TAG, "URL updated to: $newUrl")
                    Toast.makeText(this, "URL updated", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Invalid URL entered")
                    Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (customView != null) {
            Log.d(TAG, "Exiting fullscreen mode via back press")
            customViewCallback?.onCustomViewHidden()
        } else if (webView.canGoBack()) {
            Log.d(TAG, "Navigating back in WebView history")
            webView.goBack()
        } else {
            Log.d(TAG, "Exiting app via back press")
            super.onBackPressed()
        }
    }
}