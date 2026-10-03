package dev.touchgrass.app.block

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Shows "Time's up" over the app, then sends the user home. The home intent is started while the
 * overlay is visible, because a visible overlay is what allows a background activity start.
 */
class AppBlocker(private val context: Context) {
    private val overlay = BlockOverlay(context)

    suspend fun block(packageName: String) {
        if (!Settings.canDrawOverlays(context)) return
        try {
            withContext(Dispatchers.Main) { overlay.show(appLabel(packageName)) }
            delay(MESSAGE_DURATION)
            context.startActivity(homeIntent())
            delay(HOME_TRANSITION)
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { overlay.hide() }
        }
    }

    private fun appLabel(packageName: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun homeIntent() = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        val MESSAGE_DURATION = 2.seconds
        val HOME_TRANSITION = 500.milliseconds
    }
}
