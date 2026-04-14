package com.aodreplacement

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XSharedPreferences

object AodBridge {

    private val handler = Handler(Looper.getMainLooper())

    private val prefs: XSharedPreferences by lazy {
        XSharedPreferences("com.aodreplacement", "module_prefs").apply {
            makeWorldReadable()
        }
    }

    private fun targetPackage(): String {
        prefs.reload()
        return prefs.getString("target_package", "com.paget96.batteryguru") ?: "com.paget96.batteryguru"
    }

    private fun targetComponent(): String {
        prefs.reload()
        return prefs.getString("target_component", "com.paget96.batteryguru.aod.overlay.AODOverlayActivity")
            ?: "com.paget96.batteryguru.aod.overlay.AODOverlayActivity"
    }

    private fun launchMethod(): String {
        prefs.reload()
        return prefs.getString("launch_method", "activity") ?: "activity"
    }

    fun launchAod(context: Context) {
        handler.postDelayed({
            val pkg = targetPackage()
            val component = targetComponent()
            val method = launchMethod()

            // Try Intent-based launch first (works from SystemUI system process)
            try {
                val intent = Intent().apply {
                    this.component = ComponentName(pkg, component)
                    action = "$pkg.ACTION_START_AOD"
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }

                when (method) {
                    "activity" -> {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        MainHook.log("Started AOD activity: $component")
                    }
                    else -> {
                        context.startForegroundService(intent)
                        MainHook.log("Started AOD service: $component")
                    }
                }
                return@postDelayed
            } catch (e: Exception) {
                MainHook.log("Intent launch failed, trying am command: ${e.message}")
            }

            // Fallback: use am command (works from system process context)
            try {
                val cmd = when (method) {
                    "activity" -> "am start -n $pkg/$component"
                    else -> "am startservice -n $pkg/$component"
                }
                Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                MainHook.log("Launched via am command: $cmd")
            } catch (e: Exception) {
                MainHook.log("am command fallback also failed: ${e.message}")
            }
        }, 150)
    }

    fun dismissAod(context: Context) {
        val pkg = targetPackage()
        val component = targetComponent()
        val method = launchMethod()

        try {
            when (method) {
                "activity" -> {
                    // Send stop broadcast for activities
                    val broadcastIntent = Intent("$pkg.ACTION_STOP_AOD").apply {
                        setPackage(pkg)
                    }
                    context.sendBroadcast(broadcastIntent)
                    // Also force-stop the activity via am
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "am force-stop $pkg"))
                    MainHook.log("Dismissed AOD activity: $pkg")
                }
                else -> {
                    try {
                        val intent = Intent().apply {
                            this.component = ComponentName(pkg, component)
                            action = "$pkg.ACTION_STOP_AOD"
                        }
                        context.startService(intent)
                        MainHook.log("Sent stop to AOD service: $component")
                    } catch (e: Exception) {
                        Runtime.getRuntime().exec(arrayOf("sh", "-c", "am stopservice -n $pkg/$component"))
                        MainHook.log("Stopped service via am command")
                    }
                }
            }
        } catch (e: Exception) {
            MainHook.log("AOD dismiss failed: ${e.message}")
        }
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(targetPackage(), 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
