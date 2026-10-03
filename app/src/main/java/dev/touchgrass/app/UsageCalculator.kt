package dev.touchgrass.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/** Today's foreground time per package, plus the app that is in the foreground right now. */
data class UsageToday(
    val usageMs: Map<String, Long>,
    val foregroundPackage: String?,
)

/**
 * Computes today's usage from raw usage events (local midnight to now), shared by the UI and the
 * monitoring service. queryUsageStats aggregates are deliberately not used; they are unreliable.
 */
class UsageCalculator(context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun today(now: Long = System.currentTimeMillis()): UsageToday {
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = usageStatsManager.queryEvents(midnight, now)

        val usage = HashMap<String, Long>()
        val resumed = HashMap<String, MutableSet<String>>()
        val since = HashMap<String, Long>()
        val seen = HashSet<String>()
        var foreground: String? = null

        fun close(pkg: String, at: Long) {
            val start = since.remove(pkg) ?: return
            resumed.remove(pkg)
            usage[pkg] = (usage[pkg] ?: 0L) + (at - start)
            if (foreground == pkg) foreground = null
        }

        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            val pkg = event.packageName
            val time = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val classes = resumed.getOrPut(pkg) { mutableSetOf() }
                    if (classes.isEmpty()) since[pkg] = time
                    classes += event.className.orEmpty()
                    foreground = pkg
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val classes = resumed[pkg]
                    if (classes == null) {
                        if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED && pkg !in seen) {
                            usage[pkg] = (usage[pkg] ?: 0L) + (time - midnight)
                        }
                    } else {
                        classes -= event.className.orEmpty()
                        if (classes.isEmpty()) close(pkg, time)
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    since.keys.toList().forEach { close(it, time) }
                }
            }
            if (pkg != null) seen += pkg
        }

        since.keys.toList().forEach { pkg ->
            val stillForeground = foreground == pkg
            close(pkg, now)
            if (stillForeground) foreground = pkg
        }

        return UsageToday(usage, foreground)
    }
}
