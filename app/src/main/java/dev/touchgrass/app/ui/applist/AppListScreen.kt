package dev.touchgrass.app.ui.applist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.touchgrass.app.R
import kotlinx.coroutines.flow.drop

@Composable
fun AppListScreen(viewModel: AppListViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingPackage by rememberSaveable { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    AppListContent(state, viewModel.query, onAppClick = { editingPackage = it.app.packageName })

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
private fun AppListContent(state: AppListState, query: TextFieldState, onAppClick: (AppUsage) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(query) {
        snapshotFlow { query.text.toString() }.drop(1).collect { listState.scrollToItem(0) }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
                SearchField(query)
            }
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.searching && state.apps.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_apps_found))
            }
        } else {
            LazyColumn(state = listState, contentPadding = innerPadding) {
                items(state.apps, key = { it.app.packageName }) { item ->
                    AppRow(item, onClick = { onAppClick(item) })
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: TextFieldState) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        state = query,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_apps)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.text.isNotEmpty()) {
                IconButton(onClick = { query.clearText() }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.search_clear))
                }
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboard?.hide() },
    )
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
