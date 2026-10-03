package dev.touchgrass.app.monitor

import android.content.Context
import android.content.Intent
import dev.touchgrass.app.limits.LimitStore
import dev.touchgrass.app.permissions.Permissions

object Monitor {
    fun sync(context: Context) {
        val intent = Intent(context, MonitorService::class.java)
        if (shouldRun(context)) {
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    private fun shouldRun(context: Context): Boolean =
        Permissions.allGranted(context) && LimitStore(context).all().isNotEmpty()
}
