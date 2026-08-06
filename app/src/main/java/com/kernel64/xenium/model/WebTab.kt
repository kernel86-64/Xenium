package com.kernel64.xenium.model

import android.webkit.WebView
import java.util.UUID

data class WebTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val hasError: Boolean = false,
    val errorDescription: String = "",
    val isDesktopMode: Boolean = false,
    val isIncognito: Boolean = false,
    val themeColor: Int? = null,
    val webView: WebView? = null
)
