package dev.touchgrass.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.touchgrass.app.LaunchableApp
import dev.touchgrass.app.LaunchableApps
import dev.touchgrass.app.R
import dev.touchgrass.app.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IconSize = 40.dp

private data class AppRow(val app: LaunchableApp, val usageMinutes: Long)

/** Launchable apps with today's usage. Reloaded whenever [refreshKey] changes (on every resume). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(refreshKey: Int) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { IconSize.roundToPx() }
    val rows by produceState<List<AppRow>?>(initialValue = null, refreshKey) {
        value = withContext(Dispatchers.Default) {
            val usage = UsageCalculator(context).today().usageMs
            LaunchableApps.load(context, iconSizePx)
                .map { AppRow(it, (usage[it.packageName] ?: 0L) / 60_000) }
                .sortedBy { it.app.label.lowercase() }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val list = rows
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = innerPadding) {
                items(list, key = { it.app.packageName }) { row ->
                    ListItem(
                        headlineContent = { Text(row.app.label) },
                        supportingContent = { Text(stringResource(R.string.usage_minutes, row.usageMinutes)) },
                        leadingContent = {
                            Image(row.app.icon, contentDescription = null, modifier = Modifier.size(IconSize))
                        },
                    )
                }
            }
        }
    }
}
