package com.kernel64.xenium.util

import android.content.Context
import com.kernel64.xenium.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

object HistoryHelper {
    private const val PREFS_NAME = "XeniumHistory"
    private const val KEY_HISTORY_LIST = "history_list"
    private const val MAX_HISTORY_ITEMS = 500

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY_LIST, null) ?: return emptyList()
        val result = mutableListOf<HistoryItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title")
                val url = obj.optString("url")
                val timestamp = obj.optLong("timestamp")
                if (url.isNotBlank()) {
                    result.add(HistoryItem(id = id, title = title, url = url, timestamp = timestamp))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun addHistoryItem(context: Context, title: String, url: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("javascript:") || url.startsWith("data:")) {
            return
        }

        val cleanTitle = if (title.isBlank() || title == "about:blank" || title == url) {
            try {
                URI(url).host ?: url
            } catch (e: Exception) {
                url
            }
        } else {
            title
        }

        val currentList = getHistory(context).toMutableList()

        // Don't add duplicate entry if the most recent item is identical in URL
        if (currentList.isNotEmpty() && currentList.first().url == url) {
            // Update title/timestamp if title improved
            if (currentList.first().title != cleanTitle) {
                currentList[0] = currentList.first().copy(title = cleanTitle, timestamp = System.currentTimeMillis())
                saveHistory(context, currentList)
            }
            return
        }

        val newItem = HistoryItem(title = cleanTitle, url = url, timestamp = System.currentTimeMillis())
        currentList.add(0, newItem)

        if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.removeAt(currentList.size - 1)
        }

        saveHistory(context, currentList)
    }

    fun deleteHistoryItem(context: Context, id: String) {
        val currentList = getHistory(context).filter { it.id != id }
        saveHistory(context, currentList)
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY_LIST).apply()
    }

    private fun saveHistory(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY_LIST, jsonArray.toString()).apply()
    }
}
