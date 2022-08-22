package com.udacity.main

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.AndroidViewModel
import com.udacity.R
import com.udacity.details.DetailActivity
import com.udacity.models.DownloadStatus
import com.udacity.models.URL
import com.udacity.util.DOWNLOAD_STATUS_INTENT_EXTRA_KEY
import com.udacity.util.FILE_NAME_INTENT_EXTRA_KEY

private const val NOTIFICATION_ID = 0
private const val CHANNEL_ID = "channelId"

class MainActivityViewModel(
    private val app: Application,
    private val downloadManager: DownloadManager,
    private val notificationManager: NotificationManager
) : AndroidViewModel(app) {

    fun download(urlModel: URL): Long {
        val request = DownloadManager.Request(Uri.parse(urlModel.uri))
            .setTitle(app.getString(R.string.app_name))
            .setDescription(app.getString(R.string.app_description))
            .setRequiresCharging(false)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "/${urlModel.title}.zip"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    fun createNotification(selectedDownloadUri: URL, downloadStatus: DownloadStatus) {
        val detailIntent = Intent(app, DetailActivity::class.java)
        detailIntent.putExtra(FILE_NAME_INTENT_EXTRA_KEY, selectedDownloadUri.title)
        detailIntent.putExtra(DOWNLOAD_STATUS_INTENT_EXTRA_KEY, downloadStatus)

        val pendingIntent = TaskStackBuilder.create(app).run {
            addNextIntentWithParentStack(detailIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        } as PendingIntent

        val action = NotificationCompat.Action(
            R.drawable.ic_assistant_black_24dp,
            app.getString(R.string.notification_button),
            pendingIntent
        )

        val contentIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            putExtra(DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            app,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(selectedDownloadUri.title)
            .setContentText(selectedDownloadUri.text)
            .setContentIntent(contentPendingIntent)
            .addAction(action)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                app.getString(R.string.load_app_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
            }
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(true)
            notificationChannel.description =
                app.getString(R.string.load_app_notification_channel_description)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}