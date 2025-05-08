package com.watch.animekai

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.net.http.SslError
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var customView: View? = null
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isNetworkAvailable = true

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
                // SSL error detected, show "No Network Available" screen
                handler?.cancel()
                showNoNetworkScreen()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // General WebView error detected, show "No Network Available" screen
                if (request?.isForMainFrame == true) {
                    showNoNetworkScreen()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                // Enter full-screen mode
                customView = view
                customViewContainer?.visibility = View.VISIBLE
                customViewContainer?.addView(view)
                customViewCallback = callback
                webView.visibility = View.GONE

                // Set screen orientation to landscape
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                // Hide system UI for full-screen experience
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            override fun onHideCustomView() {
                // Exit full-screen mode
                customViewContainer?.visibility = View.GONE
                customViewContainer?.removeView(customView)
                customView = null
                customViewCallback = null
                webView.visibility = View.VISIBLE

                // Reset screen orientation to default
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                // Restore system UI
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Load the website
        webView.loadUrl("https://animekai.to/")

        // Start checking for network availability
        checkNetworkFor10Seconds()
    }

    private fun checkNetworkFor10Seconds() {
        handler.postDelayed({
            if (!isNetworkConnected()) {
                isNetworkAvailable = false
                showNoNetworkScreen()
            }
        }, 10000) // 10 seconds
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
        if (customView != null) {
            // Exit full-screen mode if active
            customViewCallback?.onCustomViewHidden()
        } else {
            super.onBackPressed()
        }
    }
}