package dev.touchgrass.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.touchgrass.app.monitor.Monitor
import dev.touchgrass.app.permissions.Permissions
import dev.touchgrass.app.ui.applist.AppListScreen
import dev.touchgrass.app.ui.permissions.PermissionScreen

@Composable
fun TouchGrassApp() {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(Permissions.firstMissing(context)) }
    val recheck = {
        missing = Permissions.firstMissing(context)
        Monitor.sync(context)
    }
    LifecycleResumeEffect(Unit) {
        recheck()
        onPauseOrDispose {}
    }

    when (val permission = missing) {
        null -> AppListScreen()
        else -> PermissionScreen(permission, onResult = recheck)
    }
}
