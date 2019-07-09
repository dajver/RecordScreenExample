package dajver.com.recordscreenexample.recorder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dajver.com.recordscreenexample.RequestMediaProjectionActivity
import dajver.com.recordscreenexample.recorder.helpers.ScreenRecorderHelper
import dajver.com.recordscreenexample.recorder.service.listeners.RecordListener
import dajver.com.recordscreenexample.recorder.service.listeners.RecordingPermissionListener
import dajver.com.recordscreenexample.recorder.service.listeners.StopRecordingListener

import java.io.File
import java.io.IOException

class RecordService : Service(), RecordListener {

    private var mStartRecordingListener: RecordingPermissionListener? = null
    private var mStopRecordingListener: StopRecordingListener? = null
    private var mSystemStopRecordingListener: StopRecordingListener? = null
    private var mScreenRecorderHelper: ScreenRecorderHelper? = null

    private val mIBinder = RecordServiceBinder()

    private val isRecording: Boolean get() = mScreenRecorderHelper!!.isRecording

    override fun onCreate() {
        super.onCreate()
        mScreenRecorderHelper = ScreenRecorderHelper(this)
        mScreenRecorderHelper!!.setRecordListener(this)
    }

    override fun onDestroy() {
        mScreenRecorderHelper!!.setRecordListener(null)
        mScreenRecorderHelper!!.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra(ACTION_CODE_EXTRA_DATA)) {
            when (intent.getIntExtra(ACTION_CODE_EXTRA_DATA, -1)) {
                ACTION_START_RECORDING -> onActionStartRecording(intent)
                ACTION_STOP_RECORDING -> onActionStopRecording()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return mIBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        stopThis()
        return super.onUnbind(intent)
    }

    override fun onStartRecording() {
        if (mStartRecordingListener != null)
            mStartRecordingListener!!.onPermissionGranted()
    }

    override fun onStopRecording() {
        if (mStopRecordingListener != null) {
            mStopRecordingListener!!.onStopRecording()
            stopThis()
        } else if (mSystemStopRecordingListener != null) {
            mSystemStopRecordingListener!!.onSystemStopRecording()
            stopThis()
        }
    }

    private fun onActionStartRecording(intent: Intent) {
        if (!intent.hasExtra(OUTPUT_FILE_PATH_EXTRA_DATA)) {
            return
        }

        if (intent.hasExtra(STATE_RESULT_CODE) && intent.hasExtra(STATE_RESULT_DATA)) {
            val resultCode = intent.getIntExtra(STATE_RESULT_CODE, 0)
            val resultData = intent.getParcelableExtra<Intent>(STATE_RESULT_DATA)

            // Checking the action from the user, Granted or Denied permission
            if (!mScreenRecorderHelper!!.onScreenRecordPermissionResult(resultCode, resultData)) {
                if (mStartRecordingListener != null)
                    mStartRecordingListener!!.onPermissionDenied()

                stopThis()

                return
            }
        }

        startRecording(intent.getStringExtra(OUTPUT_FILE_PATH_EXTRA_DATA))
    }

    private fun onActionStopRecording() {
        stopRecording()
    }

    private fun startRecording(outputFilePath: String?) {
        if (hasScreenRecordPermission()) {
            try {
                mScreenRecorderHelper!!.startRecording(File(outputFilePath!!))
            } catch (e: IOException) {
                e.printStackTrace()
                stopThis()
            } catch (ignore: IllegalStateException) { }
        } else {
            // Requesting media projection
            requestPermission(outputFilePath)
        }
    }

    private fun stopRecording() {
        mScreenRecorderHelper!!.stopRecording()
    }

    private fun hasScreenRecordPermission(): Boolean {
        return mScreenRecorderHelper!!.hasScreenRecordPermission()
    }

    private fun requestPermission(outputFilePath: String?) {
        // Requesting media projection
        startActivity(RequestMediaProjectionActivity.createIntent(this@RecordService, ACTION_START_RECORDING, outputFilePath!!))
    }

    private fun stopThis() {
        stopForeground(true)
        stopSelf()
    }

    inner class RecordServiceBinder : Binder() {
        fun startRecording(outputFilePath: String, startRecordingListener: RecordingPermissionListener, stopCurrentIfRecording: Boolean, systemStopRecordingListener: StopRecordingListener) {
            if (stopCurrentIfRecording && this@RecordService.isRecording) {
                this@RecordService.stopRecording()
            }

            mStartRecordingListener = startRecordingListener
            mSystemStopRecordingListener = systemStopRecordingListener
            this@RecordService.startRecording(outputFilePath)
        }

        fun stopRecording(listener: StopRecordingListener) {
            mStopRecordingListener = listener
            this@RecordService.stopRecording()
        }
    }

    companion object {

        private const val STATE_RESULT_CODE = "result_code"
        private const val STATE_RESULT_DATA = "result_data"

        private const val OUTPUT_FILE_PATH_EXTRA_DATA = "output_file_path"
        private const val ACTION_CODE_EXTRA_DATA = "action_code"

        const val ACTION_START_RECORDING = 0
        const val ACTION_STOP_RECORDING = 1

        fun createIntent(context: Context, actionCode: Int, outputFilePath: String, resultCode: Int, resultData: Intent): Intent {
            val intent = Intent(context, RecordService::class.java)
            intent.putExtra(ACTION_CODE_EXTRA_DATA, actionCode)
            intent.putExtra(OUTPUT_FILE_PATH_EXTRA_DATA, outputFilePath)
            intent.putExtra(STATE_RESULT_CODE, resultCode)
            intent.putExtra(STATE_RESULT_DATA, resultData)
            return intent
        }
    }
}