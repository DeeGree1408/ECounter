package com.dg.electricitycounter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // До Android 13 разрешение не требуется
            true
        }
    }
    
    fun requestNotificationPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(context)) {
                // Запрашиваем разрешение
                (context as? android.app.Activity)?.let { activity ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        100
                    )
                }
            }
        }
    }
}