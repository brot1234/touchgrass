package dev.touchgrass.app.apps

import java.text.Normalizer

interface ListedApp {
    val label: String
    val usageMs: Long
    val hasLimit: Boolean
}

fun <T : ListedApp> List<T>.forAppList(query: String): List<T> {
    val needle = normalize(query.trim())
    return filter { needle.isEmpty() || normalize(it.label).contains(needle) }
        .sortedWith(
            compareBy<T> { !it.hasLimit }
                .thenByDescending { it.usageMs }
                .thenBy { it.label.lowercase() },
        )
}

private val combiningMarks = Regex("\\p{Mn}+")

private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()
