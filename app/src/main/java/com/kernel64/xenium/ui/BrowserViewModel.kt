package com.kernel64.xenium.ui

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import com.kernel64.xenium.model.HistoryItem
import com.kernel64.xenium.model.WebTab
import com.kernel64.xenium.util.HistoryHelper
import com.kernel64.xenium.util.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val _tabs = MutableStateFlow<List<WebTab>>(emptyList())
    val tabs: StateFlow<List<WebTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _showTabsSheet = MutableStateFlow(false)
    val showTabsSheet: StateFlow<Boolean> = _showTabsSheet.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        // Create initial default tab
        val initialUrl = SettingsHelper.getSearchEngine(getApplication()).homepageUrl
        addNewTab(initialUrl)
        loadHistory()
    }

    val activeTab: WebTab?
        get() = _tabs.value.find { it.id == _activeTabId.value } ?: _tabs.value.firstOrNull()

    fun openOrNavigateToUrl(url: String) {
        if (url.isBlank()) return
        val currentActive = activeTab
        val homeUrl = SettingsHelper.getSearchEngine(getApplication()).homepageUrl
        if (currentActive != null && (currentActive.url == homeUrl || currentActive.url.isBlank())) {
            updateTabError(currentActive.id, false)
            updateTabUrl(currentActive.id, url)
            currentActive.webView?.loadUrl(url)
        } else {
            addNewTab(url)
        }
    }

    fun addNewTab(url: String? = null) {
        val finalUrl = url ?: SettingsHelper.getSearchEngine(getApplication()).homepageUrl
        val newTab = WebTab(url = finalUrl)
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
    }

    fun addNewIncognitoTab(url: String? = null) {
        val finalUrl = url ?: SettingsHelper.getSearchEngine(getApplication()).homepageUrl
        val newTab = WebTab(url = finalUrl, isIncognito = true)
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabToClose = currentTabs.find { it.id == tabId }
        tabToClose?.webView?.let { wv ->
            try {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                if (tabToClose.isIncognito) {
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()
                    wv.clearSslPreferences()
                }
                wv.onPause()
                wv.removeAllViews()
                wv.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (currentTabs.size <= 1) {
            // If last tab is closed, reset it to home instead of empty screen
            val newTab = WebTab(url = SettingsHelper.getSearchEngine(getApplication()).homepageUrl)
            _tabs.value = listOf(newTab)
            _activeTabId.value = newTab.id
            return
        }

        val tabIndex = currentTabs.indexOfFirst { it.id == tabId }
        val updatedTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = updatedTabs

        if (_activeTabId.value == tabId) {
            val newActiveIndex = if (tabIndex > 0) tabIndex - 1 else 0
            _activeTabId.value = updatedTabs.getOrNull(newActiveIndex)?.id ?: ""
        }
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun setShowTabsSheet(show: Boolean) {
        _showTabsSheet.value = show
    }

    fun loadHistory() {
        _history.value = HistoryHelper.getHistory(getApplication())
    }

    fun recordHistory(tabId: String, title: String, url: String) {
        val targetTab = _tabs.value.find { it.id == tabId }
        if (targetTab?.isIncognito == true) return
        if (url.isBlank() || url == "about:blank" || url.startsWith("javascript:") || url.startsWith("data:")) return
        HistoryHelper.addHistoryItem(getApplication(), title, url)
        loadHistory()
    }

    fun deleteHistoryItem(id: String) {
        HistoryHelper.deleteHistoryItem(getApplication(), id)
        loadHistory()
    }

    fun clearAllHistory() {
        HistoryHelper.clearHistory(getApplication())
        loadHistory()
    }

    fun updateTabUrl(tabId: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        val currentTab = _tabs.value.find { it.id == tabId }
        if (currentTab?.url == url) return
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(url = url) else it }
        }
        currentTab?.let { recordHistory(tabId, it.title, url) }
    }

    fun updateTabError(tabId: String, hasError: Boolean, errorDescription: String = "") {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(hasError = hasError, errorDescription = errorDescription) else it }
        }
    }

    fun updateTabTitle(tabId: String, title: String) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(title = title) else it }
        }
        val currentTab = _tabs.value.find { it.id == tabId }
        currentTab?.let { recordHistory(tabId, title, it.url) }
    }

    fun updateTabThemeColor(tabId: String, color: Int?) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(themeColor = color) else it }
        }
    }

    fun updateTabProgress(tabId: String, progress: Int) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(progress = progress, isLoading = progress < 100) else it }
        }
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(isLoading = isLoading) else it }
        }
    }

    fun updateTabNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(canGoBack = canGoBack, canGoForward = canGoForward) else it }
        }
    }

    fun setTabWebView(tabId: String, webView: WebView?) {
        _tabs.update { tabs ->
            tabs.map { if (it.id == tabId) it.copy(webView = webView) else it }
        }
    }

    fun toggleTabDesktopMode(tabId: String) {
        _tabs.update { tabs ->
            tabs.map { tab ->
                if (tab.id == tabId) {
                    val newDesktopMode = !tab.isDesktopMode
                    tab.webView?.let { webView ->
                        val isCustomUaEnabled = SettingsHelper.isCustomUaEnabled(webView.context)
                        val baseUserAgent = if (isCustomUaEnabled) {
                            SettingsHelper.getEffectiveCustomUa(webView.context)
                        } else {
                            android.webkit.WebSettings.getDefaultUserAgent(webView.context)
                        }
                        webView.settings.userAgentString = if (newDesktopMode) {
                            baseUserAgent.replace("Mobile", "", ignoreCase = true)
                                .replace("Android", "Linux", ignoreCase = true)
                        } else {
                            baseUserAgent
                        }
                        webView.reload()
                    }
                    tab.copy(isDesktopMode = newDesktopMode)
                } else tab
            }
        }
    }

    fun updateAllTabsUserAgent(context: android.content.Context) {
        val isCustomUaEnabled = SettingsHelper.isCustomUaEnabled(context)
        val baseUserAgent = if (isCustomUaEnabled) {
            SettingsHelper.getEffectiveCustomUa(context)
        } else {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        }
        
        _tabs.value.forEach { tab ->
            tab.webView?.let { webView ->
                val newUa = if (tab.isDesktopMode) {
                    baseUserAgent.replace("Mobile", "", ignoreCase = true)
                        .replace("Android", "Linux", ignoreCase = true)
                } else {
                    baseUserAgent
                }
                if (webView.settings.userAgentString != newUa) {
                    webView.settings.userAgentString = newUa
                    webView.reload()
                }
            }
        }
    }
}
