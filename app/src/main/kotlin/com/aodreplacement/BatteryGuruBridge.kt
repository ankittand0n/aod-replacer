package com.aodreplacement

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

object BatteryGuruBridge {

    private const val BATTERY_GURU_PACKAGE = "com.paget96.batteryguru"
    // Battery Guru AOD service — this is the known entry point for its custom AOD
    private const val BATTERY_GURU_AOD_SERVICE = "$BATTERY_GURU_PACKAGE.services.AodService"

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Launch Battery Guru's custom AOD.
     * Called after screen-off with a short delay to let the system settle.
     */
    fun launchAod(context: Context) {
        handler.postDelayed({
            try {
                // Try starting the AOD service directly
                val serviceIntent = Intent().apply {
                    component = ComponentName(BATTERY_GURU_PACKAGE, BATTERY_GURU_AOD_SERVICE)
                    action = "com.paget96.batteryguru.ACTION_START_AOD"
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                context.startForegroundService(serviceIntent)
                MainHook.log("Sent start AOD intent to Battery Guru service")
            } catch (e: Exception) {
                MainHook.log("Battery Guru service start failed: ${e.message}")
                // Fallback: send broadcast that Battery Guru might listen for
                tryBroadcastFallback(context, start = true)
            }
        }, 150) // Small delay to let screen fully turn off
    }

    /**
     * Dismiss Battery Guru's custom AOD.
     * Called when screen turns on or user unlocks.
     */
    fun dismissAod(context: Context) {
        try {
            val serviceIntent = Intent().apply {
                component = ComponentName(BATTERY_GURU_PACKAGE, BATTERY_GURU_AOD_SERVICE)
                action = "com.paget96.batteryguru.ACTION_STOP_AOD"
            }
            context.startService(serviceIntent)
            MainHook.log("Sent stop AOD intent to Battery Guru service")
        } catch (e: Exception) {
            MainHook.log("Battery Guru service stop failed: ${e.message}")
            tryBroadcastFallback(context, start = false)
        }
    }

    private fun tryBroadcastFallback(context: Context, start: Boolean) {
        try {
            val action = if (start) {
                "com.paget96.batteryguru.ACTION_START_AOD"
            } else {
                "com.paget96.batteryguru.ACTION_STOP_AOD"
            }
            val broadcastIntent = Intent(action).apply {
                setPackage(BATTERY_GURU_PACKAGE)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(broadcastIntent)
            MainHook.log("Sent fallback broadcast: $action")
        } catch (e: Exception) {
            MainHook.log("Battery Guru not installed or unreachable: ${e.message}")
        }
    }

    /**
     * Check if Battery Guru is installed on the device.
     */
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(BATTERY_GURU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
