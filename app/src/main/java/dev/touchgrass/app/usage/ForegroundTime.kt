package dev.touchgrass.app.usage

import dev.touchgrass.app.usage.UsageEvent.Type

/** Sums foreground time per app. An app is in use while at least one of its activities is resumed. */
object ForegroundTime {
    fun calculate(events: Sequence<UsageEvent>, from: Long, to: Long): UsageToday {
        val tracker = Tracker(from)
        events.forEach(tracker::apply)
        return tracker.finish(to)
    }

    private class Tracker(private val from: Long) {
        private val usage = HashMap<String, Long>()
        private val resumedActivities = HashMap<String, MutableSet<String>>()
        private val inUseSince = HashMap<String, Long>()
        private val seen = HashSet<String>()
        private var foreground: String? = null

        fun apply(event: UsageEvent) {
            when (event.type) {
                Type.Resumed -> resume(event)
                Type.Paused, Type.Stopped -> pause(event)
                Type.ScreenOff -> closeAll(event.time)
            }
            seen += event.packageName
        }

        fun finish(to: Long): UsageToday {
            val current = foreground
            closeAll(to)
            return UsageToday(usage.toMap(), current)
        }

        private fun resume(event: UsageEvent) {
            val activities = resumedActivities.getOrPut(event.packageName) { mutableSetOf() }
            if (activities.isEmpty()) inUseSince[event.packageName] = event.time
            activities += event.className
            foreground = event.packageName
        }

        private fun pause(event: UsageEvent) {
            val activities = resumedActivities[event.packageName]
            if (activities == null) {
                if (event.type == Type.Paused && event.packageName !in seen) {
                    add(event.packageName, event.time - from)
                }
                return
            }
            activities -= event.className
            if (activities.isEmpty()) close(event.packageName, event.time)
        }

        private fun close(packageName: String, at: Long) {
            val start = inUseSince.remove(packageName) ?: return
            resumedActivities.remove(packageName)
            add(packageName, at - start)
            if (foreground == packageName) foreground = null
        }

        private fun closeAll(at: Long) {
            inUseSince.keys.toList().forEach { close(it, at) }
        }

        private fun add(packageName: String, durationMs: Long) {
            usage[packageName] = (usage[packageName] ?: 0L) + durationMs
        }
    }
}
