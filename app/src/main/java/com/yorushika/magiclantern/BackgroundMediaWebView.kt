package com.yorushika.magiclantern

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

class BackgroundMediaWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        if (windowVisibility == View.VISIBLE) {
            return
        }
        super.dispatchWindowVisibilityChanged(View.VISIBLE)
    }
}