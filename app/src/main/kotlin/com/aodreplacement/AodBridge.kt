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
        return prefs.getString("target_component", "com.paget96.batteryguru.services.AodService")
            ?: "com.paget96.batteryguru.services.AodService"
    }

    private fun launchMethod(): String {
        prefs.reload()
        return prefs.getString("launch_method", "service") ?: "service"
    }

    fun launchAod(context: Context) {
        handler.postDelayed({
            try {
                val pkg = targetPackage()
                val component = targetComponent()
                val method = launchMethod()
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
            } catch (e: Exception) {
                MainHook.log("AOD launch failed: ${e.message}")
            }
        }, 150)
    }

    fun dismissAod(context: Context) {
        try {
            val pkg = targetPackage()
            val component = targetComponent()
            val method = launchMethod()
            val intent = Intent().apply {
                this.component = ComponentName(pkg, component)
                action = "$pkg.ACTION_STOP_AOD"
            }

            when (method) {
                "activity" -> {
                    val broadcastIntent = Intent("$pkg.ACTION_STOP_AOD").apply {
                        setPackage(pkg)
                    }
                    context.sendBroadcast(broadcastIntent)
                    MainHook.log("Sent stop broadcast to AOD activity package: $pkg")
                }
                else -> {
                    context.startService(intent)
                    MainHook.log("Sent stop to AOD service: $component")
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
