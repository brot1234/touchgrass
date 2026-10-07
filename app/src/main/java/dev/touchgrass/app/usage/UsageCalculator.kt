package dev.touchgrass.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/** Today's usage from raw usage events. queryUsageStats aggregates are not used; they are unreliable. */
class UsageCalculator(context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun today(now: Long = System.currentTimeMillis()): UsageToday {
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ForegroundTime.calculate(queryEvents(midnight - LOOKBACK_MS, now), midnight, now)
    }

    private fun queryEvents(from: Long, to: Long): Sequence<UsageEvent> = sequence {
        val events = usageStatsManager.queryEvents(from, to)
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            val type = event.toType() ?: continue
            yield(UsageEvent(type, event.packageName, event.className.orEmpty(), event.timeStamp))
        }
    }

    private fun UsageEvents.Event.toType(): UsageEvent.Type? = when (eventType) {
        UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.Resumed
        UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.Paused
        UsageEvents.Event.ACTIVITY_STOPPED -> UsageEvent.Type.Stopped
        UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEvent.Type.ScreenOff
        UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEvent.Type.ScreenOn
        else -> null
    }

    private companion object {
        const val LOOKBACK_MS = 6 * 60 * 60 * 1000L
    }
}
