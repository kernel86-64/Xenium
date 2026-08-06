package com.kernel64.xenium.util

import android.content.Context

enum class SearchEngine(val title: String, val urlTemplate: String, val homepageUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com")
}

enum class UaType {
    PREDEFINED, CUSTOM
}

object SettingsHelper {
    private const val PREFS_NAME = "XeniumSettings"
    private const val KEY_SEARCH_ENGINE = "search_engine"
    private const val KEY_CUSTOM_UA_ENABLED = "custom_ua_enabled"
    private const val KEY_CUSTOM_UA_TYPE = "custom_ua_type"
    private const val KEY_CUSTOM_UA_VALUE = "custom_ua_value"
    private const val KEY_SPOOF_VISIBILITY = "spoof_visibility"
    
    const val PREDEFINED_UA = "Mozilla/5.0 (Linux; Android 17; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.7871.129 Mobile Safari/537.36"

    fun getSearchEngine(context: Context): SearchEngine {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val engineName = prefs.getString(KEY_SEARCH_ENGINE, SearchEngine.GOOGLE.name)
        return try {
            SearchEngine.valueOf(engineName!!)
        } catch (e: Exception) {
            SearchEngine.GOOGLE
        }
    }

    fun setSearchEngine(context: Context, engine: SearchEngine) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SEARCH_ENGINE, engine.name).apply()
    }

    fun isCustomUaEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CUSTOM_UA_ENABLED, false)
    }

    fun setCustomUaEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CUSTOM_UA_ENABLED, enabled).apply()
    }

    fun getUaType(context: Context): UaType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeName = prefs.getString(KEY_CUSTOM_UA_TYPE, UaType.PREDEFINED.name)
        return try {
            UaType.valueOf(typeName!!)
        } catch (e: Exception) {
            UaType.PREDEFINED
        }
    }

    fun setUaType(context: Context, type: UaType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_UA_TYPE, type.name).apply()
    }

    fun getCustomUaValue(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_UA_VALUE, "") ?: ""
    }

    fun setCustomUaValue(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CUSTOM_UA_VALUE, value).apply()
    }

    fun getEffectiveCustomUa(context: Context): String {
        return if (getUaType(context) == UaType.PREDEFINED) {
            PREDEFINED_UA
        } else {
            getCustomUaValue(context).takeIf { it.isNotBlank() } ?: PREDEFINED_UA
        }
    }

    fun isSpoofVisibilityEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SPOOF_VISIBILITY, false)
    }

    fun setSpoofVisibilityEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SPOOF_VISIBILITY, enabled).apply()
    }
}
