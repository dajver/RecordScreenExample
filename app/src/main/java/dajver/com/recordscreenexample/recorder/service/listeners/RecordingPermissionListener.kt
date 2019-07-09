package dajver.com.recordscreenexample.recorder.service.listeners

interface RecordingPermissionListener {
    fun onPermissionGranted()

    fun onPermissionDenied()
}
