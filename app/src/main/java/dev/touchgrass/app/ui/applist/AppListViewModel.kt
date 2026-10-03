package dev.touchgrass.app.ui.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.touchgrass.app.R
import dev.touchgrass.app.apps.LaunchableApp
import dev.touchgrass.app.apps.LaunchableApps
import dev.touchgrass.app.usage.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

data class AppUsage(val app: LaunchableApp, val usageMinutes: Long)

data class AppListState(
    val loading: Boolean = true,
    val apps: List<AppUsage> = emptyList(),
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val usageCalculator = UsageCalculator(application)
    private val iconSizePx = application.resources.getDimensionPixelSize(R.dimen.app_icon_size)
    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val apps = withContext(Dispatchers.Default) { loadApps() }
            _state.value = AppListState(loading = false, apps = apps)
        }
    }

    private fun loadApps(): List<AppUsage> {
        val usage = usageCalculator.today().usageMs
        return LaunchableApps.load(getApplication<Application>(), iconSizePx)
            .map { AppUsage(it, (usage[it.packageName] ?: 0L).milliseconds.inWholeMinutes) }
            .sortedBy { it.app.label.lowercase() }
    }
}
