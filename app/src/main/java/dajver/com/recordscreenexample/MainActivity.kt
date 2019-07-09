package dajver.com.recordscreenexample

import android.Manifest.permission
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dajver.com.recordscreenexample.recorder.RecorderManager
import dajver.com.recordscreenexample.recorder.service.listeners.RecordingPermissionListener
import dajver.com.recordscreenexample.recorder.service.listeners.StopRecordingListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity(), RecordingPermissionListener, StopRecordingListener,
    MultiplePermissionsListener {

    private var mRecorderManager: RecorderManager? = null

    private var isRecording: Boolean = false
    private var videoPath: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Dexter.withActivity(this).withPermissions(
                permission.WRITE_EXTERNAL_STORAGE,
                permission.READ_EXTERNAL_STORAGE,
                permission.RECORD_AUDIO
            ).withListener(this).check()

        mRecorderManager = RecorderManager(this, this, this)

        startStopButton.setOnClickListener {
            if(isRecording) {
                startStopButton.text = "Start recording"

                mRecorderManager!!.stopRecording(this)

                isRecording = false
            } else {
                startStopButton.text = "Stop recording"

                videoPath = getVideoFilePath()
                mRecorderManager!!.startRecording(videoPath!!)

                isRecording = true
            }
        }
    }

    private fun getVideoFilePath(): File {
        val videoId = UUID.randomUUID().toString()
        val rootSessionsDir = File(getExternalFilesDir(Environment.DIRECTORY_DCIM), ROOT_SESSIONS_NAME)
        if (!rootSessionsDir.exists() || !rootSessionsDir.isDirectory) {
            rootSessionsDir.mkdir()
        }
        val videoDir = File(rootSessionsDir, videoId)
        if (!videoDir.exists() || !videoDir.isDirectory) {
            videoDir.mkdir()
        }
        val gameName = String.format(Locale.US, GAME_FILE_NAME_FORMAT, (1..9999).random())
        val videoFileName = "$gameName.mp4"
        val videoFile = File(videoDir, videoFileName)
        if (!videoFile.exists() || !videoFile.isFile) {
            videoFile.createNewFile()
        }
        return videoFile
    }

    override fun onPermissionGranted() {
        // show some indicator that recording has started
    }

    override fun onPermissionDenied() {
        mRecorderManager!!.cancelRecording()
    }

    override fun onStopRecording() {
        // show some indicator that recording ended, for example show the notification about recorded video
        // e("Filepath", getVideoFilePath().absolutePath)
        createNotification()
    }

    override fun onSystemStopRecording() {
        mRecorderManager!!.cancelRecording()
        // and remove all other objectives which you need
    }

    private fun createNotification() {
        val channelId = "app_channel"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoPath!!.path))
        intent.setDataAndType(Uri.parse(videoPath!!.path), "video/mp4")

        val resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AppExampleChannel", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = getString(R.string.app_name)
            channel.setSound(null, null)
            channel.enableLights(false)
            channel.lightColor = Color.BLUE
            channel.enableVibration(false)

            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
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

    override fun onPermissionsChecked(report: MultiplePermissionsReport?) { }

    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) { }

    companion object {
        private const val GAME_FILE_NAME_FORMAT = "video_%1\$d"
        private const val ROOT_SESSIONS_NAME = "sessions"
    }
}
