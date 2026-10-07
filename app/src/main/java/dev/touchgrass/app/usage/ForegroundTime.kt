package dev.touchgrass.app.usage

import dev.touchgrass.app.usage.UsageEvent.Type

/** Sums foreground time per app. An app is in use while at least one of its activities is resumed and the screen is on. */
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
        private var screenOn = true
        private var firstScreenOff: Long? = null

        fun apply(event: UsageEvent) {
            when (event.type) {
                Type.Resumed -> resume(event)
                Type.Paused, Type.Stopped -> pause(event)
                Type.ScreenOff -> screenOff(event.time)
                Type.ScreenOn -> screenOn(event.time)
            }
            seen += event.packageName
        }

        fun finish(to: Long): UsageToday {
            val current = foreground.takeIf { screenOn }
            inUseSince.keys.toList().forEach { stop(it, to) }
            return UsageToday(usage.toMap(), current)
        }

        private fun resume(event: UsageEvent) {
            if (!screenOn) screenOn(event.time)
            resumedActivities.getOrPut(event.packageName) { mutableSetOf() } += event.className
            inUseSince.putIfAbsent(event.packageName, event.time)
            foreground = event.packageName
        }

        private fun pause(event: UsageEvent) {
            val activities = resumedActivities[event.packageName]
            if (activities == null) {
                if (event.type == Type.Paused && event.packageName !in seen) {
                    add(event.packageName, from, minOf(event.time, firstScreenOff ?: event.time))
                }
                return
            }
            activities -= event.className
            if (activities.isNotEmpty()) return
            resumedActivities.remove(event.packageName)
            stop(event.packageName, event.time)
            if (foreground == event.packageName) foreground = null
        }

        private fun screenOff(at: Long) {
            if (firstScreenOff == null) firstScreenOff = at
            inUseSince.keys.toList().forEach { stop(it, at) }
            screenOn = false
        }

        private fun screenOn(at: Long) {
            screenOn = true
            resumedActivities.keys.forEach { inUseSince.putIfAbsent(it, at) }
        }

        private fun stop(packageName: String, at: Long) {
            val start = inUseSince.remove(packageName) ?: return
            add(packageName, start, at)
        }

        private fun add(packageName: String, start: Long, end: Long) {
            val durationMs = end - maxOf(start, from)
            if (durationMs > 0) usage[packageName] = (usage[packageName] ?: 0L) + durationMs
        }
    }
}
