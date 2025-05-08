package com.watch.animekai

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var isNetworkAvailable = true
    private val defaultUrl = "https://animekai.to/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)
        customViewContainer = findViewById(R.id.customViewContainer)

        // Enable cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Configure WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                showNoNetworkScreen()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    showNoNetworkScreen()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                customViewContainer?.visibility = View.VISIBLE
                customViewContainer?.addView(view)
                customViewCallback = callback
                webView.visibility = View.GONE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            override fun onHideCustomView() {
                customViewContainer?.visibility = View.GONE
                customViewContainer?.removeView(customView)
                customView = null
                customViewCallback = null
                webView.visibility = View.VISIBLE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Load the saved URL or default URL
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("webViewUrl", defaultUrl)
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
                                showChangeUrlDialog(sharedPreferences, webView)
                            }
                        }, 5000) // 5 seconds
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isLongPress = false
                        longPressHandler.removeCallbacksAndMessages(null)
                    }
                }
                return false
            }
        })

        // Start checking for network availability
        checkNetworkFor5Seconds()
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
                    webView.loadUrl(newUrl) // Reload the WebView with the new URL
                    Toast.makeText(this, "URL updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun checkNetworkFor5Seconds() {
        handler.postDelayed({
            if (!isNetworkConnected()) {
                isNetworkAvailable = false
                showNoNetworkScreen()
            }
        }, 5000) // 5 seconds
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoNetworkScreen() {
        val intent = Intent(this, NoNetworkActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
        } else if (webView.canGoBack()) {
            webView.goBack() // Navigate back in WebView history
        } else {
            super.onBackPressed() // Exit the app
        }
    }
}