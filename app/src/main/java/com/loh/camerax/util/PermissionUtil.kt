package com.loh.camerax.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.loh.camerax.BuildConfig

class PermissionUtil {

    // private var dialog: MaterialDialog? = null

    private var launchPermissionRequest = false

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    fun register(
        activity: FragmentActivity,
        isMandatory: Boolean = false,
        onPermissionGranted: (Boolean) -> Unit
    ) {
        permissionLauncher = null
        permissionLauncher = activity.checkPermission {
            if (!it && isMandatory && !activity.isFinishing)
                showPermissionNotGrantedDialog(activity)
            else {
                onPermissionGranted.invoke(it)
            }
        }
    }

    fun register(
        fragment: Fragment,
        isMandatory: Boolean = false,
        onPermissionGranted: (Boolean) -> Unit
    ) {
        permissionLauncher = null
        permissionLauncher = fragment.checkPermission {
            if (!it && isMandatory)
                showPermissionNotGrantedDialog(fragment.context)
            else {
                onPermissionGranted.invoke(it)
            }
        }
    }

    fun checkSpecificPermission(permission: Permission) {
        permissionLauncher?.launch(permission.manifest)
    }

    fun checkAllRequiredPermission() {
        if (launchPermissionRequest) return
        launchPermissionRequest = true
        permissionLauncher?.launch(Permission.All.manifest)
    }

    private fun FragmentActivity.checkPermission(onPermissionGranted: (Boolean) -> Unit = {}): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val isGranted = !it.values.contains(false)
            onPermissionGranted.invoke(isGranted)
        }
    }

    private fun Fragment.checkPermission(onPermissionGranted: (Boolean) -> Unit = {}): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val isGranted = !it.values.contains(false)
            onPermissionGranted.invoke(isGranted)
        }
    }

    private fun showPermissionNotGrantedDialog(context: Context?) {
        /*      if (context == null) return
              try {
                  if (dialog == null) {
                      dialog = DialogUtil.createOneButtonDialog(
                          context,
                          context.getString(R.string.permission_denied_title),
                          context.getString(R.string.permission_denied_message),
                          context.getString(R.string.permission_denied_go_setting)
                      )
                          ?.cancelable(false)
                          ?.positiveButton {
                              launchPermissionRequest = false
                              context.startActivity(getSettingIntent())
                          }
                  }
                  dialog?.let {
                      if (!it.isShowing) {
                          it.show()
                      }
                  }
              } catch (e: Exception) {
              }*/
    }

    private fun getSettingIntent(): Intent {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}

sealed class Permission(val manifest: Array<String>) {
    object All : Permission(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    )

    object Location : Permission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    object Camera : Permission(arrayOf(Manifest.permission.CAMERA))
    object Video : Permission(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
}
