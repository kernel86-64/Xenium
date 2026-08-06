package com.kernel64.xenium.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.kernel64.xenium.util.XeniumDownloader
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.kernel64.xenium.model.WebTab

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: WebTab,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onThemeColorChanged: (Int?) -> Unit = {},
    onProgressChanged: (Int) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onErrorReceived: (Boolean, String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onNewTabRequested: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()



    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var contextMenuResult by remember { mutableStateOf<WebView.HitTestResult?>(null) }

    var pendingDownloadAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingDownloadAction?.invoke()
        } else {
            android.widget.Toast.makeText(context, "Storage permission is required to download files", android.widget.Toast.LENGTH_SHORT).show()
        }
        pendingDownloadAction = null
    }

    BackHandler(enabled = fullScreenView != null) {
        fullScreenCallback?.onCustomViewHidden()
    }



    val mediaObserverJs = """
        (function() {
            if (window._mediaObserverInjected) return;
            window._mediaObserverInjected = true;
            
            function reportMediaState() {
                var m = document.querySelector('video, audio');
                if (m) {
                    XeniumMediaInterop.updateMediaState(!m.paused, document.title);
                } else {
                    XeniumMediaInterop.updateMediaState(false, document.title);
                }
            }

            setInterval(reportMediaState, 1500);
        })();
    """.trimIndent()

    val visibilitySpoofJs = """
        (function() {
            if (window.__xeniumVisibilitySpoofed) return;
            window.__xeniumVisibilitySpoofed = true;
            try {
                Object.defineProperty(document, 'hidden', {
                    get: function() { return false; },
                    configurable: true
                });
                Object.defineProperty(document, 'visibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });
                Object.defineProperty(document, 'webkitHidden', {
                    get: function() { return false; },
                    configurable: true
                });
                Object.defineProperty(document, 'webkitVisibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });

                var origAEL = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                        return;
                    }
                    return origAEL.apply(this, arguments);
                };
            } catch(e) {}
    """.trimIndent()

    val privacyJs = """
        (function() {
            if (window.__xeniumPrivacyInjected) return;
            window.__xeniumPrivacyInjected = true;

            var randomBatteryLevel = Math.floor(Math.random() * 91 + 5) / 100.0;
            var randomCharging = Math.random() > 0.5;

            if (navigator.getBattery) {
                Object.defineProperty(navigator, 'getBattery', {
                    value: function() {
                        return Promise.resolve({
                            charging: randomCharging,
                            chargingTime: randomCharging ? (Math.floor(Math.random() * 5000) + 1000) : Infinity,
                            dischargingTime: randomCharging ? Infinity : (Math.floor(Math.random() * 5000) + 1000),
                            level: randomBatteryLevel,
                            onchargingchange: null,
                            onchargingtimechange: null,
                            ondischargingtimechange: null,
                            onlevelchange: null
                        });
                    },
                    configurable: true
                });
            }

            try {
                var lang = navigator.language || 'en-US';
                Object.defineProperty(navigator, 'languages', {
                    get: function() { return [lang]; },
                    configurable: true
                });
            } catch(e) {}

            if (window.DeviceOrientationEvent) {
                Object.defineProperty(window, 'DeviceOrientationEvent', { value: undefined, configurable: true });
            }
            if (window.DeviceMotionEvent) {
                Object.defineProperty(window, 'DeviceMotionEvent', { value: undefined, configurable: true });
            }
            var origAEL = window.addEventListener;
            window.addEventListener = function(type, listener, options) {
                if (type === 'deviceorientation' || type === 'devicemotion') {
                    return;
                }
                return origAEL.call(this, type, listener, options);
            };

            var randomGpu = '';
            var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
            for (var i = 0; i < 6; i++) {
                randomGpu += chars.charAt(Math.floor(Math.random() * chars.length));
            }
            
            var getParameterProxy = function(original) {
                return function(parameter) {
                    if (parameter === 37445) return 'Google Inc. (Apple)';
                    if (parameter === 37446) return randomGpu;
                    return original.apply(this, arguments);
                };
            };
            try {
                if (window.WebGLRenderingContext) {
                    window.WebGLRenderingContext.prototype.getParameter = getParameterProxy(window.WebGLRenderingContext.prototype.getParameter);
                }
                if (window.WebGL2RenderingContext) {
                    window.WebGL2RenderingContext.prototype.getParameter = getParameterProxy(window.WebGL2RenderingContext.prototype.getParameter);
                }
            } catch(e) {}

            try {
                Object.defineProperty(document, 'referrer', { get: function() { return ''; }, configurable: true });
                var meta = document.createElement('meta');
                meta.name = 'referrer';
                meta.content = 'no-referrer';
                var appendMeta = function() {
                    if (document.head) document.head.appendChild(meta);
                    else if (document.documentElement) document.documentElement.appendChild(meta);
                };
                if (document.head || document.documentElement) {
                    appendMeta();
                } else {
                    var obs = new MutationObserver(function() {
                        if (document.head || document.documentElement) {
                            appendMeta();
                            obs.disconnect();
                        }
                    });
                    obs.observe(document, {childList: true, subtree: true});
                }
            } catch(e) {}
        })();
    """.trimIndent()

    val webView = remember(tab.id) {
        tab.webView ?: object : WebView(context) {
            override fun onWindowVisibilityChanged(visibility: Int) {
                if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                    super.onWindowVisibilityChanged(android.view.View.VISIBLE)
                } else {
                    super.onWindowVisibilityChanged(visibility)
                }
            }
            override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
                if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                    super.onVisibilityChanged(changedView, android.view.View.VISIBLE)
                } else {
                    super.onVisibilityChanged(changedView, visibility)
                }
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            if (tab.isIncognito) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    try {
                        val profileStore = ProfileStore.getInstance()
                        val incognitoProfile = profileStore.getOrCreateProfile("incognito")
                        WebViewCompat.setProfile(this, "incognito")
                        val profileCookieManager = incognitoProfile.cookieManager
                        profileCookieManager.setAcceptCookie(false)
                        profileCookieManager.setAcceptThirdPartyCookies(this, false)
                        profileCookieManager.removeAllCookies(null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(false)
                    cookieManager.setAcceptThirdPartyCookies(this, false)
                    cookieManager.removeAllCookies(null)
                }
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(this, privacyJs, setOf("*"))
                if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                    WebViewCompat.addDocumentStartJavaScript(this, visibilitySpoofJs, setOf("*"))
                }
            }

            if (tab.isIncognito) {
                clearCache(true)
                clearFormData()
                clearHistory()
                clearSslPreferences()
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                    try {
                        WebViewCompat.setProfile(this, Profile.DEFAULT_PROFILE_NAME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
            }

            addJavascriptInterface(com.kernel64.xenium.util.MediaInterop(), "XeniumMediaInterop")

            setOnLongClickListener {
                val hitResult = this@apply.hitTestResult
                if (hitResult.type == WebView.HitTestResult.UNKNOWN_TYPE || hitResult.type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
                    false
                } else {
                    contextMenuResult = hitResult
                    true
                }
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !tab.isIncognito
                @Suppress("DEPRECATION")
                databaseEnabled = !tab.isIncognito
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false

                if (tab.isIncognito) {
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    saveFormData = false
                }
                
                val baseUserAgent = if (tab.isIncognito) {
                    com.kernel64.xenium.util.UserAgentGenerator.generateRandomIncognitoUserAgent()
                } else {
                    val isCustomUaEnabled = com.kernel64.xenium.util.SettingsHelper.isCustomUaEnabled(context)
                    if (isCustomUaEnabled) {
                        com.kernel64.xenium.util.SettingsHelper.getEffectiveCustomUa(context)
                    } else {
                        android.webkit.WebSettings.getDefaultUserAgent(context)
                    }
                }
                
                userAgentString = if (tab.isDesktopMode) {
                    baseUserAgent.replace("Mobile", "", ignoreCase = true)
                        .replace("Android", "Linux", ignoreCase = true)
                        .replace("iPhone", "Macintosh", ignoreCase = true)
                } else {
                    baseUserAgent
                }
                
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // Configure Force Dark on native WebSettings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = if (isDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                }
            }

            // Configure WebSettingsCompat Dark Mode features BEFORE loadUrl
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    @Suppress("DEPRECATION")
                    WebSettingsCompat.setForceDark(
                        settings,
                        if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDarkStrategy(
                        settings,
                        WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                val performDownload: () -> Unit = {
                    XeniumDownloader.downloadFile(context, url, userAgent, contentDisposition, mimetype)
                }

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingDownloadAction = performDownload
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    performDownload()
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return handleExternalScheme(view, url)
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    return handleExternalScheme(view, url)
                }

                private fun handleExternalScheme(view: WebView?, url: String): Boolean {
                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("javascript:")) {
                        return false
                    }
                    try {
                        val intent = if (url.startsWith("intent://")) {
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                component = null
                                selector = null
                            }
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        }
                        
                        try {
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            if (url.startsWith("intent://")) {
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (fallbackUrl != null) {
                                    view?.loadUrl(fallbackUrl)
                                    return true
                                }
                                val pack = intent.`package`
                                if (pack != null) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=\$pack")))
                                    } catch (e2: Exception) {
                                        e2.printStackTrace()
                                    }
                                    return true
                                }
                            }
                            return true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onThemeColorChanged(null)
                    if (url != "about:blank") {
                        onLoadingChanged(true)
                        url?.let { onUrlChanged(it) }


                        if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                            view?.evaluateJavascript(visibilitySpoofJs, null)
                        }
                        view?.evaluateJavascript(privacyJs, null)
                    }
                    view?.let {
                        onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != "about:blank") {
                        onLoadingChanged(false)
                        url?.let { onUrlChanged(it) }
                        view?.let {
                            it.title?.let { title -> onTitleChanged(title) }
                        }


                        if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                            view?.evaluateJavascript(visibilitySpoofJs, null)
                        }
                        view?.evaluateJavascript(privacyJs, null)

                        // Extract HTML meta theme-color tag
                        view?.evaluateJavascript(
                            "(function(){var m=document.querySelector('meta[name=\"theme-color\"]');return m?m.content:'';})()"
                        ) { result ->
                            val colorStr = result?.replace("\"", "")?.trim()
                            if (!colorStr.isNullOrBlank() && colorStr != "null") {
                                try {
                                    val parsedColor = android.graphics.Color.parseColor(colorStr)
                                    onThemeColorChanged(parsedColor)
                                } catch (e: Exception) {
                                    onThemeColorChanged(null)
                                }
                            } else {
                                onThemeColorChanged(null)
                            }
                        }
                    }
                    view?.let {
                        onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        val errCode = error?.errorCode ?: -1
                        val rawDesc = error?.description?.toString() ?: ""
                        val finalCode = mapErrorCode(errCode, rawDesc)

                        view?.stopLoading()
                        onErrorReceived(true, finalCode)
                    }
                }

                @Suppress("DEPRECATION")
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    if (failingUrl == null || failingUrl == tab.url || failingUrl == view?.url) {
                        val finalCode = mapErrorCode(errorCode, description)

                        view?.stopLoading()
                        onErrorReceived(true, finalCode)
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (url != "about:blank") {
                        url?.let { onUrlChanged(it) }

                        if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context)) {
                            view?.evaluateJavascript(visibilitySpoofJs, null)
                        }
                        view?.evaluateJavascript(privacyJs, null)
                        view?.evaluateJavascript(mediaObserverJs, null)
                    }
                    view?.let {
                        onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    if (fullScreenView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    fullScreenView = view
                    fullScreenCallback = callback

                    val activity = context as? Activity ?: return
                    val decorView = activity.window.decorView as FrameLayout
                    view?.let {
                        decorView.addView(it, FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                    }

                    WindowInsetsControllerCompat(activity.window, decorView).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    val view = fullScreenView ?: return
                    val activity = context as? Activity
                    if (activity != null) {
                        val decorView = activity.window.decorView as FrameLayout
                        decorView.removeView(view)

                        WindowInsetsControllerCompat(activity.window, decorView).show(WindowInsetsCompat.Type.systemBars())
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }

                    fullScreenView = null
                    fullScreenCallback?.onCustomViewHidden()
                    fullScreenCallback = null
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)

                    if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(context) && newProgress > 5) {
                        view?.evaluateJavascript(visibilitySpoofJs, null)
                    }
                    if (newProgress > 5) {
                        view?.evaluateJavascript(privacyJs, null)
                    }
                    if (newProgress > 10) {
                        view?.evaluateJavascript(mediaObserverJs, null)
                    }
                    if (newProgress > 15) {
                        view?.evaluateJavascript(
                            "(function(){var m=document.querySelector('meta[name=\"theme-color\"]');return m?m.content:'';})()"
                        ) { result ->
                            val colorStr = result?.replace("\"", "")?.trim()
                            if (!colorStr.isNullOrBlank() && colorStr != "null") {
                                try {
                                    val parsedColor = android.graphics.Color.parseColor(colorStr)
                                    onThemeColorChanged(parsedColor)
                                } catch (e: Exception) {
                                    onThemeColorChanged(null)
                                }
                            } else {
                                onThemeColorChanged(null)
                            }
                        }
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (title != "about:blank") {
                        title?.let { onTitleChanged(it) }
                    }
                }
            }

            loadUrl(tab.url)
        }
    }

    // Update dark theme settings if system theme changes at runtime
    LaunchedEffect(isDarkTheme) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                webView.settings.forceDark = if (isDarkTheme) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(
                    webView.settings,
                    if (isDarkTheme) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(tab.id) {
        onWebViewCreated(webView)
    }



    DisposableEffect(tab.id) {
        onDispose {
            // Keep webview detached clean
        }
    }

    DisposableEffect(webView) {
        val receiver = object : com.kernel64.xenium.util.MediaInteropObserver.MediaActionReceiver {
            override fun onMediaAction(action: String) {
                webView.post {
                    if (action == "play") {
                        webView.evaluateJavascript("(function(){ var elems = document.querySelectorAll('video, audio'); for (var i = 0; i < elems.length; i++) { elems[i].play().catch(function(e){}); } })();", null)
                    } else if (action == "pause") {
                        webView.evaluateJavascript("(function(){ var elems = document.querySelectorAll('video, audio'); for (var i = 0; i < elems.length; i++) { elems[i].pause(); } })();", null)
                    }
                }
            }
        }
        com.kernel64.xenium.util.MediaInteropObserver.addActionReceiver(receiver)
        onDispose {
            com.kernel64.xenium.util.MediaInteropObserver.removeActionReceiver(receiver)
        }
    }

    key(tab.id) {
        AndroidView(
            factory = {
                webView.parent?.let { (it as ViewGroup).removeView(webView) }
                webView 
            },
            modifier = modifier.fillMaxSize()
        )
    }

    // LaunchedEffect removed because it causes reloads on tab switch

    if (contextMenuResult != null) {
        Dialog(onDismissRequest = { contextMenuResult = null }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val result = contextMenuResult!!
                    val url = result.extra ?: ""
                    
                    if (url.isNotBlank()) {
                        // Dynamic M3E Action Items List
                        val actionItems = mutableListOf<Pair<String, () -> Unit>>()

                        if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            actionItems.add("Save Image" to {
                                val performDownload: () -> Unit = {
                                    XeniumDownloader.downloadFile(context, url, null, null, null)
                                    contextMenuResult = null
                                }

                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    pendingDownloadAction = performDownload
                                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    performDownload()
                                }
                            })
                        }
                        
                        if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            actionItems.add("Open in New Tab" to {
                                onNewTabRequested(url)
                                contextMenuResult = null
                            })
                        }
                        
                        actionItems.add("Copy Link" to {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                            android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                            contextMenuResult = null
                        })

                        // URL Header
                        Text(
                            text = url,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )

                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Action Items
                        actionItems.forEachIndexed { index, item ->
                            Text(
                                text = item.first,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { item.second() }
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                            if (index < actionItems.size - 1) {
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mapErrorCode(errorCode: Int, rawDescription: String?): String {
    if (!rawDescription.isNullOrBlank()) {
        val regexMatch = Regex("ERR_[A-Z0-9_]+").find(rawDescription)
        if (regexMatch != null) return regexMatch.value
    }
    return when (errorCode) {
        WebViewClient.ERROR_HOST_LOOKUP -> "ERR_NAME_NOT_RESOLVED"
        WebViewClient.ERROR_CONNECT -> "ERR_CONNECTION_REFUSED"
        WebViewClient.ERROR_TIMEOUT -> "ERR_TIMED_OUT"
        WebViewClient.ERROR_IO, WebViewClient.ERROR_UNKNOWN -> "ERR_INTERNET_DISCONNECTED"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "ERR_SSL_PROTOCOL_ERROR"
        WebViewClient.ERROR_REDIRECT_LOOP -> "ERR_TOO_MANY_REDIRECTS"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "ERR_UNSUPPORTED_SCHEME"
        else -> if (!rawDescription.isNullOrBlank()) rawDescription else "ERR_CONNECTION_REFUSED"
    }
}
