package dajver.com.recordscreenexample

import android.Manifest.permission
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dajver.com.recordscreenexample.recorder.RecorderManager
import dajver.com.recordscreenexample.recorder.service.listeners.RecordingPermissionListener
import dajver.com.recordscreenexample.recorder.service.listeners.StopRecordingListener
import dajver.com.recordscreenexample.wrappers.NotificationWrapper
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
        NotificationWrapper.createNotification(this, videoPath!!)
    }

    override fun onSystemStopRecording() {
        mRecorderManager!!.cancelRecording()
        // and remove all other objectives which you need
    }

    override fun onPermissionsChecked(report: MultiplePermissionsReport?) { }

    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) { }

    companion object {
        private const val GAME_FILE_NAME_FORMAT = "video_%1\$d"
        private const val ROOT_SESSIONS_NAME = "videos"
    }
}
