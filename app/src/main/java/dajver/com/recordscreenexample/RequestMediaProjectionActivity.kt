package dajver.com.recordscreenexample

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import dajver.com.recordscreenexample.recorder.service.RecordService

class RequestMediaProjectionActivity : AppCompatActivity() {

    private var mRequestMediaProjection = true

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION_CODE) {
            mRequestMediaProjection = false

            startService(RecordService.createIntent(this,
                    intent.getIntExtra(ACTION_CODE_EXTRA_DATA, -1),
                    intent.getStringExtra(OUTPUT_FILE_PATH_EXTRA_DATA)!!,
                    resultCode, data!!
                )
            )

            finishAndRemoveTask()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        if (mRequestMediaProjection) {
            requestScreenRecordPermission()
        } else {
            finishAndRemoveTask()
        }
    }

    private fun requestScreenRecordPermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mediaProjectionManager != null) {
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION_CODE
            )
        }
    }

    companion object {

        private const val REQUEST_MEDIA_PROJECTION_CODE = 613

        private const val ACTION_CODE_EXTRA_DATA = "action_code"
        private const val OUTPUT_FILE_PATH_EXTRA_DATA = "output_file_path"

        fun createIntent(context: Context, actionCode: Int, outputFilePath: String): Intent {
            val intent = Intent(context, RequestMediaProjectionActivity::class.java)

            intent.putExtra(ACTION_CODE_EXTRA_DATA, actionCode)
            intent.putExtra(OUTPUT_FILE_PATH_EXTRA_DATA, outputFilePath)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            return intent
        }
    }
}