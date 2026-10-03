package dev.touchgrass.app.ui.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.touchgrass.app.R
import dev.touchgrass.app.apps.LaunchableApp
import dev.touchgrass.app.apps.LaunchableApps
import dev.touchgrass.app.limits.LimitStore
import dev.touchgrass.app.usage.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

data class AppUsage(val app: LaunchableApp, val usageMinutes: Long, val limitMinutes: Int?) {
    val limitReached: Boolean get() = limitMinutes != null && usageMinutes >= limitMinutes
}

data class AppListState(
    val loading: Boolean = true,
    val apps: List<AppUsage> = emptyList(),
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {
    private val usageCalculator = UsageCalculator(application)
    private val limitStore = LimitStore(application)
    private val iconSizePx = application.resources.getDimensionPixelSize(R.dimen.app_icon_size)
    private val snapshot = MutableStateFlow<Snapshot?>(null)
    private var refreshJob: Job? = null

    val state: StateFlow<AppListState> = combine(snapshot, limitStore.changes()) { snapshot, limits ->
        if (snapshot == null) AppListState() else AppListState(loading = false, apps = snapshot.withLimits(limits))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppListState())

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            snapshot.value = withContext(Dispatchers.Default) { loadSnapshot() }
        }
    }

    fun setLimit(packageName: String, minutes: Int) = limitStore.set(packageName, minutes)

    fun removeLimit(packageName: String) = limitStore.remove(packageName)

    private fun loadSnapshot() = Snapshot(
        apps = LaunchableApps.load(getApplication<Application>(), iconSizePx),
        usageMs = usageCalculator.today().usageMs,
    )

    private class Snapshot(val apps: List<LaunchableApp>, val usageMs: Map<String, Long>) {
        fun withLimits(limits: Map<String, Int>): List<AppUsage> = apps
            .map { AppUsage(it, (usageMs[it.packageName] ?: 0L).milliseconds.inWholeMinutes, limits[it.packageName]) }
            .sortedWith(compareBy<AppUsage> { it.limitMinutes == null }.thenBy { it.app.label.lowercase() })
    }
}
