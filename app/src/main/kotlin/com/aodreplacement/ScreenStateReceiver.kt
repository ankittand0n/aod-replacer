package com.aodreplacement

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object ScreenStateReceiver {

    private var registered = false

    /**
     * Register screen state receiver inside SystemUI process.
     * We hook into Application.onCreate to get a valid Context for registering receivers.
     */
    fun register(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as Application
                        registerReceiver(app)
                    }
                }
            )
            MainHook.log("Hooked Application.onCreate for screen state registration")
        } catch (e: Throwable) {
            MainHook.log("Failed to hook Application.onCreate: ${e.message}")
        }
    }

    private fun registerReceiver(context: Context) {
        if (registered) return
        registered = true

        if (!AodBridge.isInstalled(context)) {
            MainHook.log("Target AOD app not installed — screen state receiver not registered")
            return
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        MainHook.log("Screen OFF → launching custom AOD")
                        AodBridge.launchAod(ctx)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        MainHook.log("Screen ON → dismissing custom AOD")
                        AodBridge.dismissAod(ctx)
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        MainHook.log("User present → ensuring custom AOD dismissed")
                        AodBridge.dismissAod(ctx)
                    }
                }
            }
        }

        context.registerReceiver(receiver, filter)
        MainHook.log("Screen state receiver registered in SystemUI")
    }
}
