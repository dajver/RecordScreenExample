package dajver.com.recordscreenexample.recorder.helpers

import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager
import dajver.com.recordscreenexample.recorder.enums.State

import java.io.File
import java.io.IOException

class ScreenRecorder internal constructor(private val mWindowManager: WindowManager, private val mScreenDensity: Int) {

    private val mScreenSize = Point()

    private var mOutputFile: File? = null

    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaRecorder: MediaRecorder? = null

    private var mDisplayWidth: Int = 0
    private var mDisplayHeight: Int = 0
    private var mVideoBitRate: Int = 0
    private var mVideoFrameRate: Int = 0

    private var mState = State.INITIAL

    val isRecording: Boolean get() = mState == State.RECORDING

    private val displayHeight: Int get() = if (mScreenSize.y == 0) mDisplayHeight else mScreenSize.y

    private val screenWidth: Int get() = if (mScreenSize.x == 0) mDisplayWidth else mScreenSize.x

    init {
        mWindowManager.defaultDisplay.getSize(mScreenSize)
        setupVideoQualitySettings()
    }

    @Throws(IOException::class)
    internal fun startRecord(mediaProjection: MediaProjection, outputFile: File) {
        // Checking the arguments

        mOutputFile = outputFile

        // Checking the file
        if (mOutputFile!!.isDirectory) {
            throw IllegalArgumentException("It is a directory not a file!")
        }

        if (mState != State.INITIAL) {
            throw IllegalStateException("You can start recording only on the initial state!")
        }

        // Set up media recorder
        mMediaRecorder = MediaRecorder()
        initMediaRecorder()

        // Sharing the screen
        shareScreen(mediaProjection)

        mState = State.RECORDING
    }

    internal fun stopRecord() {
        if (mState != State.RECORDING) {
            return
        }

        if (mMediaRecorder == null)
            return

        try {
            mMediaRecorder!!.stop()
        } catch (ignore: RuntimeException) { }

        stopScreenSharing()
        mMediaRecorder!!.reset()
        mMediaRecorder!!.release()
        mMediaRecorder = null

        mState = State.INITIAL
    }

    @Throws(IOException::class)
    private fun initMediaRecorder() {
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setOutputFile(mOutputFile!!.path)
        mMediaRecorder!!.setVideoSize(mDisplayWidth, mDisplayHeight)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setVideoEncodingBitRate(mVideoBitRate)
        mMediaRecorder!!.setVideoFrameRate(mVideoFrameRate)

        val rotation = mWindowManager.defaultDisplay.rotation
        val orientation = ORIENTATIONS.get(rotation + 90)
        mMediaRecorder!!.setOrientationHint(orientation)

        mMediaRecorder!!.prepare()
    }

    private fun setupVideoQualitySettings() {
        mDisplayWidth = 1280
        mDisplayHeight = 720

        mVideoBitRate = 1600000
        mVideoFrameRate = 24
    }

    private fun setUpVirtualDisplay(mediaProjection: MediaProjection) {
        mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorder",
            mDisplayWidth, mDisplayHeight, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface, null, null
        )
    }

    private fun shareScreen(mediaProjection: MediaProjection) {
        try {
            setUpVirtualDisplay(mediaProjection)
            mMediaRecorder!!.start()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay!!.release()
            mVirtualDisplay = null
        }
    }

    companion object {

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
}
