package com.kernel64.xenium.util

import android.webkit.JavascriptInterface

object MediaInteropObserver {
    var isPlaying: Boolean = false
    var currentTitle: String = ""

    private val listeners = mutableListOf<MediaStateListener>()
    private val actionReceivers = mutableSetOf<MediaActionReceiver>()

    interface MediaStateListener {
        fun onMediaStateChanged(isPlaying: Boolean, title: String)
    }

    interface MediaActionReceiver {
        fun onMediaAction(action: String) // e.g. "play", "pause"
    }

    fun addListener(listener: MediaStateListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: MediaStateListener) {
        listeners.remove(listener)
    }

    fun addActionReceiver(receiver: MediaActionReceiver) {
        actionReceivers.add(receiver)
    }

    fun removeActionReceiver(receiver: MediaActionReceiver) {
        actionReceivers.remove(receiver)
    }

    fun notifyStateChanged(playing: Boolean, title: String) {
        isPlaying = playing
        currentTitle = title
        listeners.forEach { it.onMediaStateChanged(playing, title) }
    }

    fun sendMediaAction(action: String) {
        actionReceivers.forEach { it.onMediaAction(action) }
    }
}

class MediaInterop {
    @JavascriptInterface
    fun updateMediaState(isPlaying: Boolean, title: String) {
        MediaInteropObserver.notifyStateChanged(isPlaying, title)
    }
}
