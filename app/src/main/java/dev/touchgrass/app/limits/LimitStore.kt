package dev.touchgrass.app.limits

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Daily limits in minutes per package. They reset implicitly because usage is counted from midnight. */
class LimitStore(context: Context) {
    private val prefs = context.getSharedPreferences("limits", Context.MODE_PRIVATE)

    fun all(): Map<String, Int> = prefs.all.mapNotNull { (pkg, minutes) -> (minutes as? Int)?.let { pkg to it } }.toMap()

    fun set(packageName: String, minutes: Int) {
        require(minutes in VALID_MINUTES) { "Limit must be in $VALID_MINUTES, was $minutes" }
        prefs.edit { putInt(packageName, minutes) }
    }

    fun remove(packageName: String) {
        prefs.edit { remove(packageName) }
    }

    fun changes(): Flow<Map<String, Int>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(all()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(all())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        val VALID_MINUTES = 1..24 * 60
    }
}
