package dev.touchgrass.app.usage

import dev.touchgrass.app.usage.UsageEvent.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTimeTest {
    private fun event(type: Type, time: Long, pkg: String = "insta", cls: String = "Feed") =
        UsageEvent(type, pkg, cls, time)

    private fun calculate(vararg events: UsageEvent, to: Long = 1_000) =
        ForegroundTime.calculate(events.asSequence(), from = 0, to = to)

    @Test
    fun `sums separate sessions`() {
        val result = calculate(
            event(Type.Resumed, 100), event(Type.Paused, 200),
            event(Type.Resumed, 500), event(Type.Paused, 550),
        )
        assertEquals(150L, result.usageMs["insta"])
    }

    @Test
    fun `overlapping activities of one app are counted once`() {
        val result = calculate(
            event(Type.Resumed, 100, cls = "Feed"),
            event(Type.Resumed, 150, cls = "Story"),
            event(Type.Paused, 200, cls = "Feed"),
            event(Type.Paused, 300, cls = "Story"),
        )
        assertEquals(200L, result.usageMs["insta"])
    }

    @Test
    fun `app open before the start counts from the start`() {
        val result = calculate(event(Type.Paused, 300))
        assertEquals(300L, result.usageMs["insta"])
    }

    @Test
    fun `app still open counts up to now and is in the foreground`() {
        val result = calculate(event(Type.Resumed, 900), to = 1_000)
        assertEquals(100L, result.usageMs["insta"])
        assertEquals("insta", result.foregroundPackage)
    }

    @Test
    fun `screen off ends the session even if the pause comes later`() {
        val result = calculate(
            event(Type.Resumed, 100),
            event(Type.ScreenOff, 200, pkg = "android", cls = ""),
            event(Type.Paused, 700),
        )
        assertEquals(100L, result.usageMs["insta"])
        assertNull(result.foregroundPackage)
    }

    @Test
    fun `app still open after screen off and on keeps counting`() {
        val result = calculate(
            event(Type.Resumed, 100),
            event(Type.ScreenOff, 200, pkg = "android", cls = ""),
            event(Type.ScreenOn, 300, pkg = "android", cls = ""),
            event(Type.Paused, 700),
        )
        assertEquals(500L, result.usageMs["insta"])
    }

    @Test
    fun `app opened before the start counts from the start and is in the foreground`() {
        val result = ForegroundTime.calculate(sequenceOf(event(Type.Resumed, -500)), from = 0, to = 300)
        assertEquals(300L, result.usageMs["insta"])
        assertEquals("insta", result.foregroundPackage)
    }
}
