package dev.touchgrass.app.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderTest {
    private data class App(
        override val label: String,
        override val usageMs: Long = 0,
        override val hasLimit: Boolean = false,
    ) : ListedApp

    private fun List<App>.labels(query: String = "") = forAppList(query).map { it.label }

    @Test
    fun `limited apps come first even with less usage`() {
        val apps = listOf(App("Chrome", usageMs = 9_000), App("Instagram", usageMs = 10, hasLimit = true))
        assertEquals(listOf("Instagram", "Chrome"), apps.labels())
    }

    @Test
    fun `each group is sorted by usage descending`() {
        val apps = listOf(
            App("Maps", usageMs = 100),
            App("TikTok", usageMs = 50, hasLimit = true),
            App("Chrome", usageMs = 300),
            App("Instagram", usageMs = 200, hasLimit = true),
        )
        assertEquals(listOf("Instagram", "TikTok", "Chrome", "Maps"), apps.labels())
    }

    @Test
    fun `equal usage falls back to label ignoring case`() {
        val apps = listOf(App("maps"), App("Camera"), App("Clock"))
        assertEquals(listOf("Camera", "Clock", "maps"), apps.labels())
    }

    @Test
    fun `query ignores case and accents`() {
        val apps = listOf(App("Élan"), App("Maps"), App("INSTAGRAM"))
        assertEquals(listOf("Élan"), apps.labels("ela"))
        assertEquals(listOf("INSTAGRAM"), apps.labels("insta"))
        assertEquals(listOf("Élan"), apps.labels("ÉL"))
    }

    @Test
    fun `blank query returns all apps`() {
        val apps = listOf(App("Maps"), App("Chrome"))
        assertEquals(listOf("Chrome", "Maps"), apps.labels("  "))
    }

    @Test
    fun `filtering keeps the order`() {
        val apps = listOf(
            App("Camera", usageMs = 10),
            App("Chrome", usageMs = 500),
            App("Clock", usageMs = 99, hasLimit = true),
            App("Maps", usageMs = 1_000),
        )
        assertEquals(listOf("Clock", "Chrome", "Camera"), apps.labels("c"))
    }
}
