package com.yorushika.magiclantern

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.View.OnTouchListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        var systemBars: Insets? = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBars?.let { v.setPadding(it.left, it.top, it.right, it.bottom) }
            insets
        }

        val webView = findViewById<BackgroundMediaWebView>(R.id.wv_webview)
        val baseURL = "https://www.yorushika-magiclantern.com/"
        val header = mapOf("Referer" to "https://www.less-ar.com/")
        onBackPressedDispatcher.addCallback {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                if (url.host != "www.less-ar.com") {
                    view.loadUrl(url.toString())
                }
                return true
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                window.statusBarColor = Color.WHITE
                window.navigationBarColor = Color.WHITE
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress > 30) {
                    refreshBarColor(systemBars)
                }
            }
        }
        fun onClickListener(view: View): Boolean {
            if (view !is WebView) {
                return false
            }
            val result = view.hitTestResult
            if (result.type != HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                return false
            }
            val imageURL = result.extra ?: return false
            val regex = "(?<=img_)\\d+(?=.webp)".toRegex()
            val musicIndex = regex.find(imageURL)?.value?.toIntOrNull() ?: return false
            view.loadUrl("https://www.yorushika-magiclantern.com/player/$musicIndex", header)
            return true
        }
        webView.setOnTouchListener(object : OnTouchListener {
            val FINGER_RELEASED: Int = 0
            val FINGER_TOUCHED: Int = 1
            val FINGER_DRAGGING: Int = 2
            val FINGER_UNDEFINED: Int = 3

            private var fingerState = FINGER_RELEASED


            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> fingerState =
                        if (fingerState == FINGER_RELEASED) FINGER_TOUCHED
                        else FINGER_UNDEFINED

                    MotionEvent.ACTION_UP -> if (fingerState != FINGER_DRAGGING) {
                        fingerState = FINGER_RELEASED
                        return onClickListener(view)
                    } else fingerState = FINGER_RELEASED

                    MotionEvent.ACTION_MOVE -> fingerState =
                        if (fingerState == FINGER_TOUCHED || fingerState == FINGER_DRAGGING) FINGER_DRAGGING
                        else FINGER_UNDEFINED

                    else -> fingerState = FINGER_UNDEFINED

                }
                return false
            }
        })
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(baseURL)
    }

    private fun refreshBarColor(systemBarsOrNull: Insets?) {
        val webView = findViewById<WebView>(R.id.wv_webview)
        val location = IntArray(2).also { webView.getLocationInWindow(it) }
        val width = webView.width
        val height = webView.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true)
        PixelCopy.request(
            window,
            Rect(location[0], location[1], location[0] + width, location[1] + height),
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    val systemBars = systemBarsOrNull ?: Insets.NONE
                    val statusBarPixel = systemBars.top
                    val navigationBarPixel = systemBars.bottom
                    Palette.from(bitmap).setRegion(0, 0, bitmap.width, statusBarPixel)
                        .maximumColorCount(5).generate { palette ->
                            if (palette == null) return@generate
                            val mostSwatch =
                                palette.swatches.maxByOrNull { it.population }
                            window.statusBarColor = mostSwatch?.rgb ?: Color.WHITE
                            val isDarkBackgroundColor = (mostSwatch?.rgb?.let { ColorUtils.calculateLuminance(it) } ?: 1.0) < 0.5
                            controller.isAppearanceLightStatusBars = !isDarkBackgroundColor
                        }
                    Palette.from(bitmap).setRegion(
                        0,
                        bitmap.height - navigationBarPixel,
                        bitmap.width,
                        bitmap.height
                    ).maximumColorCount(5).generate { palette ->
                        if (palette == null) return@generate
                        val mostSwatch =
                            palette.swatches.maxByOrNull { it.population }
                        window.navigationBarColor = mostSwatch?.rgb ?: Color.WHITE
                    }
                }
            },
            Handler(
                Looper.getMainLooper()
            )
        )
    }
}