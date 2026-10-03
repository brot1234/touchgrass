package dev.touchgrass.app.limits

import kotlin.time.Duration.Companion.minutes

fun isLimitReached(usageMs: Long, limitMinutes: Int): Boolean =
    usageMs >= limitMinutes.minutes.inWholeMilliseconds
