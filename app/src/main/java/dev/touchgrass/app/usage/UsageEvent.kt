package dev.touchgrass.app.usage

data class UsageEvent(
    val type: Type,
    val packageName: String,
    val className: String,
    val time: Long,
) {
    enum class Type { Resumed, Paused, Stopped, ScreenOff }
}

data class UsageToday(
    val usageMs: Map<String, Long>,
    val foregroundPackage: String?,
)
