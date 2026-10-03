package dev.touchgrass.app.ui.applist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.touchgrass.app.R

@Composable
fun AppListScreen(viewModel: AppListViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingPackage by rememberSaveable { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    AppListContent(state, onAppClick = { editingPackage = it.app.packageName })

    state.apps.find { it.app.packageName == editingPackage }?.let { editing ->
        val close = { editingPackage = null }
        LimitDialog(
            appLabel = editing.app.label,
            currentLimit = editing.limitMinutes,
            onSave = { minutes ->
                viewModel.setLimit(editing.app.packageName, minutes)
                close()
            },
            onRemove = {
                viewModel.removeLimit(editing.app.packageName)
                close()
            },
            onDismiss = close,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListContent(state: AppListState, onAppClick: (AppUsage) -> Unit) {
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
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = innerPadding) {
                items(state.apps, key = { it.app.packageName }) { item ->
                    AppRow(item, onClick = { onAppClick(item) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(item: AppUsage, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.app.label) },
        supportingContent = {
            Text(
                text = usageText(item),
                color = if (item.limitReached) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        },
        leadingContent = {
            Image(
                bitmap = item.app.icon,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.app_icon_size)),
            )
        },
    )
}

@Composable
private fun usageText(item: AppUsage): String = when (val limit = item.limitMinutes) {
    null -> stringResource(R.string.minutes, item.usageMinutes)
    else -> stringResource(R.string.usage_of_limit, item.usageMinutes, limit)
}
