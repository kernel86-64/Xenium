package com.kernel64.xenium.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object XeniumDownloader {

    fun downloadFile(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.startsWith("data:") || cleanUrl.startsWith("blob:")) {
            Toast.makeText(context, "Data/Blob URIs are not supported for downloading", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            downloadManually(context, cleanUrl, userAgent, contentDisposition, mimetype)
        } else {
            downloadWithSystemManager(context, cleanUrl, userAgent, contentDisposition, mimetype)
        }
    }

    private fun downloadWithSystemManager(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ) {
        try {
            val request = android.app.DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)

            val cleanUserAgent = userAgent?.replace("\n", "")?.replace("\r", "")
            if (!cleanUserAgent.isNullOrEmpty()) {
                request.addRequestHeader("User-Agent", cleanUserAgent)
            }

            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrEmpty()) {
                request.addRequestHeader("Cookie", cookie.replace("\n", "").replace("\r", ""))
            }

            var filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            filename = filename.replace("/", "_").replace("\\", "_")

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            request.setTitle(filename)
            request.setDescription("Downloading file...")
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            @Suppress("DEPRECATION")
            request.allowScanningByMediaScanner()
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            dm.enqueue(request)

            Toast.makeText(context, "Downloading $filename...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun downloadManually(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ) {
        val appContext = context.applicationContext
        
        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = url.hashCode()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
            }
            
            val builder = NotificationCompat.Builder(appContext, "downloads")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Starting download...")
                .setContentText(url)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                
            notificationManager.notify(notificationId, builder.build())
            
            try {
                var currentUrl = url
                var redirects = 0
                var connection: HttpURLConnection

                while (true) {
                    val urlObj = URL(currentUrl)
                    connection = urlObj.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = false

                    val cleanUserAgent = userAgent?.replace("\n", "")?.replace("\r", "")
                    if (!cleanUserAgent.isNullOrEmpty()) {
                        connection.setRequestProperty("User-Agent", cleanUserAgent)
                    }

                    val cookie = CookieManager.getInstance().getCookie(currentUrl)
                    if (!cookie.isNullOrEmpty()) {
                        connection.setRequestProperty("Cookie", cookie.replace("\n", "").replace("\r", ""))
                    }

                    connection.connect()

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308
                    ) {
                        val newUrl = connection.getHeaderField("Location")
                        if (newUrl != null) {
                            currentUrl = newUrl
                            redirects++
                            if (redirects > 5) throw Exception("Too many redirects")
                            continue
                        }
                    }
                    break
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    builder.setContentTitle("Download failed")
                        .setContentText("HTTP ${connection.responseCode}")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                    notificationManager.notify(notificationId, builder.build())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Download error: HTTP ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var filename = URLUtil.guessFileName(currentUrl, contentDisposition, mimetype)
                filename = filename.replace("/", "_").replace("\\", "_")

                builder.setContentTitle(filename)
                notificationManager.notify(notificationId, builder.build())

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                var file = File(downloadsDir, filename)
                var counter = 1
                val dotIndex = filename.lastIndexOf('.')
                val namePart = if (dotIndex != -1) filename.substring(0, dotIndex) else filename
                val extPart = if (dotIndex != -1) filename.substring(dotIndex) else ""

                while (file.exists()) {
                    file = File(downloadsDir, "$namePart-$counter$extPart")
                    counter++
                }

                val totalLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)

                var downloaded = 0L
                val buffer = ByteArray(8 * 1024)
                var bytes = inputStream.read(buffer)
                var lastUpdate = System.currentTimeMillis()

                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    downloaded += bytes

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 1000) {
                        lastUpdate = now
                        if (totalLength > 0) {
                            val progress = (downloaded * 100 / totalLength).toInt()
                            builder.setProgress(100, progress, false)
                                .setContentText("$progress%")
                        } else {
                            builder.setProgress(0, 0, true)
                                .setContentText("${downloaded / 1024} KB")
                        }
                        notificationManager.notify(notificationId, builder.build())
                    }

                    bytes = inputStream.read(buffer)
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                builder.setContentTitle("Download complete")
                    .setContentText(file.name)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                notificationManager.notify(notificationId, builder.build())

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                builder.setContentTitle("Download failed")
                    .setContentText(e.localizedMessage ?: "Unknown error")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                notificationManager.notify(notificationId, builder.build())
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
