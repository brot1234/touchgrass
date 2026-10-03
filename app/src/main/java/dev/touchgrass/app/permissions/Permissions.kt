package dev.touchgrass.app.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

    fun settingsIntent(context: Context, permission: RequiredPermission): Intent {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        return when (permission) {
            RequiredPermission.UsageAccess -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri)
            RequiredPermission.Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            RequiredPermission.Notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun hasNotifications(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
