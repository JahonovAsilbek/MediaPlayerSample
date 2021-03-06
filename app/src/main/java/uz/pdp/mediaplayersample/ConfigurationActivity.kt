package uz.pdp.mediaplayersample

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uz.pdp.mediaplayersample.storage.Config

private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
private const val TAG = "Music Widget Config"
private const val MAX_ATTEMPTS = 20

class ConfigurationActivity : AppCompatActivity() {
    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
//        android.os.Debug.waitForDebugger()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        title = null
        Config.init(this)
        setResult(Activity.RESULT_CANCELED)
        checkPermission()
    }

    private fun checkPermission() {
        Log.i(TAG, "checking permissions")

        if (needsToRequestPermissions()) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.i(TAG, "showing dialog")
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.request_permission_message)
                    .setPositiveButton(R.string.request_permission_button) { _, _ ->
                        requestPermission()
                    }
                    .show()
            } else {
                // No explanation needed; request the permission
                requestPermission()
            }
        } else {
            // Permission has already been granted
            Log.i(TAG, "permission already granted")
            finishConfigOK()
        }
    }

    private fun requestPermission() {
        Log.i(TAG, "requesting permission")
        counter++
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "on request permission result")

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    finishConfigOK()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    if (counter < MAX_ATTEMPTS) {
                        checkPermission()
                    } else {
                        // give up
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.denied_permission_messsage)
                            .setPositiveButton(R.string.request_permission_button) { _, _ ->
                                finish()
                            }
                            .show()
                    }
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun finishConfigOK() {
        Log.i(TAG, "finish config OK")
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

}

fun Context.needsToRequestPermissions(): Boolean {
    val supportsDynamicPermissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    return supportsDynamicPermissions &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
}
