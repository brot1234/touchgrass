package dev.touchgrass.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.touchgrass.app.R
import dev.touchgrass.app.RequiredPermission

/** Asks for one missing permission at a time. [onChanged] makes the caller re-check permissions. */
@Composable
fun PermissionScreen(missing: RequiredPermission, onChanged: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var notificationsBlocked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false) {
            notificationsBlocked = true
        }
        onChanged()
    }

    val (title, body) = when (missing) {
        RequiredPermission.UsageAccess -> R.string.perm_usage_title to R.string.perm_usage_body
        RequiredPermission.Overlay -> R.string.perm_overlay_title to R.string.perm_overlay_body
        RequiredPermission.Notifications -> R.string.perm_notifications_title to R.string.perm_notifications_body
    }

    val onGrant = {
        when (missing) {
            RequiredPermission.UsageAccess ->
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            RequiredPermission.Overlay ->
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null)),
                )
            RequiredPermission.Notifications ->
                if (notificationsBlocked) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                } else {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(onClick = onGrant) {
                val opensDialog = missing == RequiredPermission.Notifications && !notificationsBlocked
                Text(stringResource(if (opensDialog) R.string.perm_allow else R.string.perm_open_settings))
            }
        }
    }
}
