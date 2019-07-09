package dajver.com.recordscreenexample.recorder.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import dajver.com.recordscreenexample.recorder.service.listeners.RecordListener

import java.io.File
import java.io.IOException

class ScreenRecorderHelper(private val mContext: Context) {

    private var isStartCalled = false

    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null

    private var mScreenRecorder: ScreenRecorder? = null

    private var mRecordListener: RecordListener? = null

    val isRecording: Boolean get() = screenRecorder.isRecording

    private val screenRecorder: ScreenRecorder
        get() {
            if (mScreenRecorder == null) {
                val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)

                mScreenRecorder = ScreenRecorder(windowManager, metrics.densityDpi)
            }
            return mScreenRecorder!!
        }

    private val mediaProjectionManager: MediaProjectionManager
        get() {
            if (mMediaProjectionManager == null) {
                mMediaProjectionManager = mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            }
            return mMediaProjectionManager!!
        }

    fun setRecordListener(listener: RecordListener?) {
        mRecordListener = listener
    }

    fun release() {
        if (screenRecorder.isRecording) {
            stopRecording()
        }
        tearDownMediaProjection()
    }

    @Throws(IOException::class)
    fun startRecording(outputFile: File) {
        if (!isStartCalled)
            isStartCalled = true
        else
            return

        screenRecorder.startRecord(mMediaProjection!!, outputFile)

        mRecordListener!!.onStartRecording()
    }

    fun stopRecording() {
        screenRecorder.stopRecord()

        onStopRecording()
    }

    fun hasScreenRecordPermission(): Boolean {
        return mMediaProjection != null
    }

    fun onScreenRecordPermissionResult(resultCode: Int, resultData: Intent): Boolean {
        return if (resultCode == Activity.RESULT_OK) {
            // Set up media projection
            mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            mMediaProjectionCallback = MediaProjectionCallback()
            mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)

            true
        } else {
            false
        }
    }

    private fun onStopRecording() {
        isStartCalled = false

        if (mRecordListener != null) {
            mRecordListener!!.onStopRecording()
        }
    }

    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            if (screenRecorder.isRecording) {
                try {
                    stopRecording()
                } catch (ignore: RuntimeException) {
                    // Handle cleanup here, see:
                    // https://stackoverflow.com/questions/16221866/mediarecorder-failed-when-i-stop-the-recording
                }

            }
            tearDownMediaProjection()
        }
    }
}
