package dev.touchgrass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import dev.touchgrass.app.ui.AppListScreen
import dev.touchgrass.app.ui.PermissionScreen
import dev.touchgrass.app.ui.theme.TouchGrassTheme

class MainActivity : ComponentActivity() {
    private val resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TouchGrassTheme {
                val refreshKey = resumeCount.intValue
                val missing = remember(refreshKey) { Permissions.firstMissing(this@MainActivity) }
                if (missing != null) {
                    PermissionScreen(missing, onChanged = { resumeCount.intValue++ })
                } else {
                    AppListScreen(refreshKey)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++
    }
}
