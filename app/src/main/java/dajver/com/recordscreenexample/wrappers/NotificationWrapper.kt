package dajver.com.recordscreenexample.wrappers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import dajver.com.recordscreenexample.R
import java.io.File

object NotificationWrapper {

    internal fun createNotification(context: Context, videoPath: File) {
        val channelId = "app_channel"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoPath!!.path))
        intent.setDataAndType(Uri.parse(videoPath.path), "video/mp4")

        val resultPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AppExampleChannel", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.app_name)
            channel.setSound(null, null)
            channel.enableLights(false)
            channel.lightColor = Color.BLUE
            channel.enableVibration(false)

            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Screen was recorded")
            .setContentText("You can open recorded video by clicking on this notification")
            .setContentIntent(resultPendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVibrate(null)
            .setDefaults(0)
            .setWhen(0)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(777, notification)
    }
}
