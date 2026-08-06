package com.kernel64.xenium.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.kernel64.xenium.MainActivity
import com.kernel64.xenium.R

class BackgroundPlaybackService : Service(), MediaInteropObserver.MediaStateListener {

    private var mediaSession: MediaSessionCompat? = null
    private val NOTIFICATION_ID = 10101
    private val CHANNEL_ID = "xenium_media_playback"

    private val mediaReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                "com.kernel64.xenium.ACTION_PLAY" -> MediaInteropObserver.sendMediaAction("play")
                "com.kernel64.xenium.ACTION_PAUSE" -> MediaInteropObserver.sendMediaAction("pause")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = android.content.IntentFilter().apply {
            addAction("com.kernel64.xenium.ACTION_PLAY")
            addAction("com.kernel64.xenium.ACTION_PAUSE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mediaReceiver, filter)
        }

        mediaSession = MediaSessionCompat(this, "XeniumMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    MediaInteropObserver.sendMediaAction("play")
                }
                override fun onPause() {
                    MediaInteropObserver.sendMediaAction("pause")
                }
            })
            isActive = true
        }

        MediaInteropObserver.addListener(this)
        
        // Start foreground immediately
        startForegroundServiceWithNotification(MediaInteropObserver.isPlaying, MediaInteropObserver.currentTitle)
    }

    private fun startForegroundServiceWithNotification(isPlaying: Boolean, title: String) {
        val displayTitle = if (title.isBlank()) "Media Playback" else title

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        val monColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.getColor(this, android.R.color.system_neutral1_900)
        } else {
            0xFF1C1B1F.toInt() // Dark neutral fallback
        }

        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(monColor)

        val metadata = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, displayTitle)
            .putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .build()
        mediaSession?.setMetadata(metadata)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent("com.kernel64.xenium.ACTION_PLAY").setPackage(packageName)
        val pauseIntent = Intent("com.kernel64.xenium.ACTION_PAUSE").setPackage(packageName)

        val pPlay = PendingIntent.getBroadcast(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pPause = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pPause).build()
        } else {
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Play", pPlay).build()
        }


        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .setColor(monColor)
            .setColorized(true)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onMediaStateChanged(isPlaying: Boolean, title: String) {
        startForegroundServiceWithNotification(isPlaying, title)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification(MediaInteropObserver.isPlaying, MediaInteropObserver.currentTitle)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(mediaReceiver)
        MediaInteropObserver.removeListener(this)
        mediaSession?.isActive = false
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Media Playback"
            val descriptionText = "Controls for background media playback"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
