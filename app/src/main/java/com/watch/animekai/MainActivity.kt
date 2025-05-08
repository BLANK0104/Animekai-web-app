package com.watch.animekai

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var customView: View? = null
    private var customViewContainer: FrameLayout? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

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
        webView.webViewClient = WebViewClient()
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