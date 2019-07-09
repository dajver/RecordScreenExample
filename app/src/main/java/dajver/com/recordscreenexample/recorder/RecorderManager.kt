package dajver.com.recordscreenexample.recorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dajver.com.recordscreenexample.recorder.service.RecordService
import dajver.com.recordscreenexample.recorder.service.listeners.RecordingPermissionListener
import dajver.com.recordscreenexample.recorder.service.listeners.StopRecordingListener

import java.io.File

class RecorderManager(private val mContext: Context, private val mRecordingPermissionListener: RecordingPermissionListener, private val mSystemStopRecordingListener: StopRecordingListener) {

    private var mRecordServiceBinder: RecordService.RecordServiceBinder? = null

    private var mIsBound = false
    private var mStartRecordingCalled = false

    private var mFileToUpload: File? = null

    private val mRecordServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            mRecordServiceBinder = iBinder as RecordService.RecordServiceBinder
            if (mStartRecordingCalled) {
                mStartRecordingCalled = false
                mRecordServiceBinder!!.startRecording(mFileToUpload!!.absolutePath, mRecordingPermissionListener, true, mSystemStopRecordingListener)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mRecordServiceBinder = null
            mIsBound = false
        }
    }

    fun startRecording(fileToRecord: File) {
        this.mFileToUpload = fileToRecord
        if (mRecordServiceBinder == null) {
            mStartRecordingCalled = true
            bindRecordService()
        } else {
            mRecordServiceBinder!!.startRecording(fileToRecord.absolutePath, mRecordingPermissionListener, true, mSystemStopRecordingListener)
        }
    }

    fun pauseRecording(listener: StopRecordingListener) {
        if (!mStartRecordingCalled) {
            mRecordServiceBinder!!.stopRecording(listener)
        }
    }

    fun resumeRecording(fileToRecord: File) {
        this.mFileToUpload = fileToRecord
        if (mRecordServiceBinder != null) {
            mRecordServiceBinder!!.startRecording(fileToRecord.absolutePath, mRecordingPermissionListener, true, mSystemStopRecordingListener)
        }
    }

    fun stopRecording(listener: StopRecordingListener) {
        mStartRecordingCalled = false

        if(mRecordServiceBinder != null) {
            mRecordServiceBinder!!.stopRecording(listener)
        }

        unbindRecordService()
    }

    fun cancelRecording() {
        mStartRecordingCalled = false

        unbindRecordService()
    }

    private fun bindRecordService() {
        val serviceIntent = Intent(mContext, RecordService::class.java)
        mContext.bindService(serviceIntent, mRecordServiceConnection, Context.BIND_AUTO_CREATE)
        mIsBound = true
    }

    private fun unbindRecordService() {
        if (mIsBound && mRecordServiceBinder != null) {
            mContext.unbindService(mRecordServiceConnection)
            mIsBound = false
        }

        mRecordServiceBinder = null
    }
}
