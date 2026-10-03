package dev.touchgrass.app

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

enum class RequiredPermission { UsageAccess, Overlay, Notifications }

object Permissions {
    fun firstMissing(context: Context): RequiredPermission? = when {
        !hasUsageAccess(context) -> RequiredPermission.UsageAccess
        !Settings.canDrawOverlays(context) -> RequiredPermission.Overlay
        !hasNotifications(context) -> RequiredPermission.Notifications
        else -> null
    }

    fun allGranted(context: Context): Boolean = firstMissing(context) == null

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun hasNotifications(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
