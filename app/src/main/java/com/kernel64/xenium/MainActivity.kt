package com.kernel64.xenium

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import com.kernel64.xenium.ui.BrowserViewModel
import com.kernel64.xenium.ui.components.AdaptiveTabBar
import com.kernel64.xenium.ui.components.BottomAddressBar
import com.kernel64.xenium.ui.components.BrowserWebView
import com.kernel64.xenium.ui.components.CustomErrorScreen
import com.kernel64.xenium.ui.components.TabsBottomSheet
import com.kernel64.xenium.ui.components.HistoryScreen
import com.kernel64.xenium.ui.components.SettingsScreen
import com.kernel64.xenium.ui.theme.XeniumTheme
import com.kernel64.xenium.util.SettingsHelper
import com.kernel64.xenium.util.UrlUtils

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            XeniumTheme {
                BrowserApp(
                    viewModel = viewModel,
                    onRequestDefaultBrowser = { requestDefaultBrowserRole() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        android.webkit.CookieManager.getInstance().flush()
        if (com.kernel64.xenium.util.SettingsHelper.isSpoofVisibilityEnabled(this) && 
            com.kernel64.xenium.util.MediaInteropObserver.isPlaying) {
            val intent = Intent(this, com.kernel64.xenium.util.BackgroundPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, com.kernel64.xenium.util.BackgroundPlaybackService::class.java)
        stopService(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data
        if (Intent.ACTION_VIEW == action && data != null) {
            val urlString = data.toString()
            if (urlString.isNotBlank()) {
                viewModel.openOrNavigateToUrl(urlString)
            }
        }
    }

    fun requestDefaultBrowserRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                    startActivity(intent)
                }
            }
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(fallbackIntent)
            }
        }
    }
}

@Composable
fun BrowserApp(
    viewModel: BrowserViewModel,
    onRequestDefaultBrowser: () -> Unit = {}
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val showTabsSheet by viewModel.showTabsSheet.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    // Status bar recoloring based on site theme color
    val context = LocalContext.current
    val view = LocalView.current
    val defaultStatusBarColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val siteThemeColorInt = activeTab?.themeColor
    val statusBarColor = if (siteThemeColorInt != null) Color(siteThemeColorInt) else defaultStatusBarColor

    SideEffect {
        val window = (context as? Activity)?.window
        window?.let { w ->
            w.statusBarColor = statusBarColor.toArgb()
            val isLightBackground = ColorUtils.calculateLuminance(statusBarColor.toArgb()) > 0.5
            WindowInsetsControllerCompat(w, view).isAppearanceLightStatusBars = isLightBackground
        }
    }

    // Handle system back button for WebView navigation
    BackHandler(enabled = showHistory || showSettings || activeTab != null) {
        if (showHistory) {
            showHistory = false
        } else if (showSettings) {
            showSettings = false
            viewModel.updateAllTabsUserAgent(context)
        } else {
            val webView = activeTab?.webView
            if (webView != null && webView.canGoBack()) {
                webView.goBack()
            } else if (tabs.size > 1 && activeTab != null) {
                viewModel.closeTab(activeTab.id)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Status bar header background colored in site's theme color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusBarColor)
                    .statusBarsPadding()
            )

            // Main Web View Content Area / Custom Error Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                tabs.forEach { tab ->
                    val isActive = tab.id == activeTabId
                    key(tab.id) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (isActive) 1f else 0f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (isActive && !tab.hasError) 1f else 0f)
                            ) {
                                BrowserWebView(
                                    tab = tab,
                                    onUrlChanged = { newUrl ->
                                        viewModel.updateTabUrl(tab.id, newUrl)
                                    },
                                    onTitleChanged = { newTitle ->
                                        viewModel.updateTabTitle(tab.id, newTitle)
                                    },
                                    onThemeColorChanged = { color ->
                                        if (isActive) viewModel.updateTabThemeColor(tab.id, color)
                                    },
                                    onProgressChanged = { progress ->
                                        viewModel.updateTabProgress(tab.id, progress)
                                    },
                                    onLoadingChanged = { isLoading ->
                                        viewModel.updateTabLoading(tab.id, isLoading)
                                    },
                                    onErrorReceived = { hasError, desc ->
                                        viewModel.updateTabError(tab.id, hasError, desc)
                                    },
                                    onNavigationStateChanged = { canBack, canForward ->
                                        viewModel.updateTabNavigationState(tab.id, canBack, canForward)
                                    },
                                    onWebViewCreated = { webView ->
                                        viewModel.setTabWebView(tab.id, webView)
                                    },
                                    onNewTabRequested = { url ->
                                        viewModel.addNewTab(url)
                                    }
                                )
                            }

                            if (tab.hasError && isActive) {
                                CustomErrorScreen(
                                    url = tab.url,
                                    errorDescription = tab.errorDescription,
                                    onTryAgain = {
                                        viewModel.updateTabError(tab.id, false)
                                        tab.webView?.reload() ?: viewModel.updateTabUrl(tab.id, tab.url)
                                    },
                                    onSearchGoogle = { searchQuery ->
                                        val searchEngine = SettingsHelper.getSearchEngine(context)
                                        val searchUrl = String.format(searchEngine.urlTemplate, java.net.URLEncoder.encode(searchQuery, "UTF-8"))
                                        viewModel.updateTabError(tab.id, false)
                                        viewModel.updateTabUrl(tab.id, searchUrl)
                                        tab.webView?.loadUrl(searchUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Adaptive Tabs Bar (Positioned directly above bottom address bar)
            AdaptiveTabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { tabId -> viewModel.selectTab(tabId) },
                onTabClosed = { tabId -> viewModel.closeTab(tabId) },
                onNewTab = { viewModel.addNewTab(SettingsHelper.getSearchEngine(context).homepageUrl) },
                onNewIncognitoTab = { viewModel.addNewIncognitoTab(SettingsHelper.getSearchEngine(context).homepageUrl) },
                onOpenTabsSheet = { viewModel.setShowTabsSheet(true) }
            )

            // Bottom Address Bar (Navigation bars padding applied once at bottom)
            BottomAddressBar(
                tab = activeTab,
                onNavigateToUrl = { rawInput ->
                    activeTab?.let { currentTab ->
                        val searchEngine = SettingsHelper.getSearchEngine(context)
                        val targetUrl = UrlUtils.processInputToUrl(rawInput, searchEngine)
                        viewModel.updateTabError(currentTab.id, false)
                        viewModel.updateTabUrl(currentTab.id, targetUrl)
                        currentTab.webView?.loadUrl(targetUrl)
                    }
                },
                onBackClicked = {
                    activeTab?.let {
                        viewModel.updateTabError(it.id, false)
                        it.webView?.goBack()
                    }
                },
                onForwardClicked = {
                    activeTab?.let {
                        viewModel.updateTabError(it.id, false)
                        it.webView?.goForward()
                    }
                },
                onRefreshClicked = {
                    activeTab?.let {
                        viewModel.updateTabError(it.id, false)
                        it.webView?.reload()
                    }
                },
                onStopClicked = {
                    activeTab?.webView?.stopLoading()
                },
                onSettingsClicked = { showSettings = true },
                onHistoryClicked = { showHistory = true },
                onDesktopSiteToggled = {
                    activeTab?.let { viewModel.toggleTabDesktopMode(it.id) }
                }
            )
        }

        // Tabs Modal Bottom Sheet
        if (showTabsSheet) {
            TabsBottomSheet(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { tabId ->
                    viewModel.selectTab(tabId)
                    viewModel.setShowTabsSheet(false)
                },
                onTabClosed = { tabId -> viewModel.closeTab(tabId) },
                onNewTab = {
                    viewModel.addNewTab(SettingsHelper.getSearchEngine(context).homepageUrl)
                    viewModel.setShowTabsSheet(false)
                },
                onNewIncognitoTab = {
                    viewModel.addNewIncognitoTab(SettingsHelper.getSearchEngine(context).homepageUrl)
                    viewModel.setShowTabsSheet(false)
                },
                onDismiss = { viewModel.setShowTabsSheet(false) }
            )
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = showSettings,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300))
        ) {
            SettingsScreen(onNavigateBack = { 
                showSettings = false 
                viewModel.updateAllTabsUserAgent(context)
            })
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showHistory,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300))
        ) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { showHistory = false },
                onUrlSelected = { selectedUrl ->
                    showHistory = false
                    activeTab?.let { currentTab ->
                        viewModel.updateTabError(currentTab.id, false)
                        viewModel.updateTabUrl(currentTab.id, selectedUrl)
                        currentTab.webView?.loadUrl(selectedUrl)
                    }
                }
            )
        }
    }
}