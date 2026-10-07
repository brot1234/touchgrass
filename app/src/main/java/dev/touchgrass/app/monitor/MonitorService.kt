package dev.touchgrass.app.monitor

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import dev.touchgrass.app.block.AppBlocker
import dev.touchgrass.app.limits.LimitStore
import dev.touchgrass.app.limits.isLimitReached
import dev.touchgrass.app.usage.UsageCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** Polls the foreground app while the screen is on and blocks it once it reaches its limit. */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private lateinit var usageCalculator: UsageCalculator
    private lateinit var limitStore: LimitStore
    private lateinit var blocker: AppBlocker

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startPolling()
                Intent.ACTION_SCREEN_OFF -> stopPolling()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageCalculator = UsageCalculator(this)
        limitStore = LimitStore(this)
        blocker = AppBlocker(this)
        val screenEvents = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenEvents, RECEIVER_NOT_EXPORTED)
        if (getSystemService(PowerManager::class.java).isInteractive) startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MonitorNotification.ID, MonitorNotification.create(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                checkForeground()
                delay(1.seconds)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun checkForeground() {
        val overLimit = foregroundAppOverLimit() ?: return
        blocker.block(overLimit)
    }

    private fun foregroundAppOverLimit(): String? {
        val usage = usageCalculator.today()
        val foreground = usage.foregroundPackage ?: return null
        val limit = limitStore.all()[foreground] ?: return null
        return foreground.takeIf { isLimitReached(usage.usageMs[foreground] ?: 0L, limit) }
    }
}
