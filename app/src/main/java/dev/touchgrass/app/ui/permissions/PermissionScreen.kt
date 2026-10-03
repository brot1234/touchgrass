package dev.touchgrass.app.ui.permissions

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.touchgrass.app.R
import dev.touchgrass.app.permissions.Permissions
import dev.touchgrass.app.permissions.RequiredPermission
import dev.touchgrass.app.ui.theme.TouchGrassTheme

@Composable
fun PermissionScreen(missing: RequiredPermission, onResult: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    // Once the system stops showing the notification dialog, fall back to the settings page
    var notificationDialogBlocked by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false) {
            notificationDialogBlocked = true
        }
        onResult()
    }
    val usesDialog = missing == RequiredPermission.Notifications && !notificationDialogBlocked

    PermissionPrompt(
        title = missing.title,
        body = missing.body,
        button = if (usesDialog) R.string.perm_allow else R.string.perm_open_settings,
        onClick = {
            if (usesDialog) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(Permissions.settingsIntent(context, missing))
            }
        },
    )
}

@Composable
private fun PermissionPrompt(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes button: Int,
    onClick: () -> Unit,
) {
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
            Button(onClick = onClick) {
                Text(stringResource(button))
            }
        }
    }
}

private val RequiredPermission.title: Int
    get() = when (this) {
        RequiredPermission.UsageAccess -> R.string.perm_usage_title
        RequiredPermission.Overlay -> R.string.perm_overlay_title
        RequiredPermission.Notifications -> R.string.perm_notifications_title
    }

private val RequiredPermission.body: Int
    get() = when (this) {
        RequiredPermission.UsageAccess -> R.string.perm_usage_body
        RequiredPermission.Overlay -> R.string.perm_overlay_body
        RequiredPermission.Notifications -> R.string.perm_notifications_body
    }

@Preview(showBackground = true)
@Composable
private fun PermissionPromptPreview() {
    TouchGrassTheme {
        PermissionPrompt(R.string.perm_usage_title, R.string.perm_usage_body, R.string.perm_open_settings) {}
    }
}
