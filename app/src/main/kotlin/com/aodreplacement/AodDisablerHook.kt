package com.aodreplacement

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AodDisablerHook {

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        try {
            val ambientConfigClass = XposedHelpers.findClass(
                "android.hardware.display.AmbientDisplayConfiguration",
                classLoader
            )

            // Hook alwaysOnEnabled(int user) → return false
            XposedHelpers.findAndHookMethod(
                ambientConfigClass,
                "alwaysOnEnabled",
                Int::class.javaPrimitiveType,
                XC_MethodReplacement.returnConstant(false)
            )
            MainHook.log("Hooked AmbientDisplayConfiguration.alwaysOnEnabled() → false")

            // Hook alwaysOnAvailable() → return false
            // This hides the AOD toggle in system settings so user doesn't get confused
            XposedHelpers.findAndHookMethod(
                ambientConfigClass,
                "alwaysOnAvailable",
                XC_MethodReplacement.returnConstant(false)
            )
            MainHook.log("Hooked AmbientDisplayConfiguration.alwaysOnAvailable() → false")

        } catch (e: Throwable) {
            MainHook.log("Error hooking AmbientDisplayConfiguration: ${e.message}")
        }

        // Also suppress doze AOD triggers in DozeScreenState if present
        try {
            val dozeScreenStateClass = XposedHelpers.findClass(
                "com.android.systemui.doze.DozeScreenState",
                classLoader
            )

            XposedHelpers.findAndHookMethod(
                dozeScreenStateClass,
                "transitionTo",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                object : de.robv.android.xposed.XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val newState = param.args[1] as Int
                        // android.view.Display.STATE_DOZE_SUSPEND = 4, STATE_DOZE = 3
                        // Prevent transition to doze display states (system AOD rendering)
                        if (newState == 3 || newState == 4) {
                            MainHook.log("Blocking doze screen state transition to $newState")
                            param.result = null
                        }
                    }
                }
            )
            MainHook.log("Hooked DozeScreenState.transitionTo()")
        } catch (e: Throwable) {
            // DozeScreenState may not exist or have different signature — non-fatal
            MainHook.log("DozeScreenState hook skipped: ${e.message}")
        }
    }
}
