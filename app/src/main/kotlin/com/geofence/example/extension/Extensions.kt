package com.geofence.example.extension

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


fun <T> lazyN(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun Activity.askForPermission(requestCode: Int, permission: String) {
    ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
}

fun Activity.withPermission(requestCode: Int, permission: String, permissionGranted: () -> Unit) {
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        askForPermission(requestCode, permission)
    } else {
        permissionGranted()
    }
}