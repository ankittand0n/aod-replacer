package com.aodreplacement

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    companion object {
        const val TAG = "AodReplacer"
        const val PACKAGE_SYSTEMUI = "com.android.systemui"

        private val prefs: XSharedPreferences by lazy {
            XSharedPreferences("com.aodreplacement", "module_prefs").apply {
                makeWorldReadable()
            }
        }

        fun isEnabled(): Boolean {
            prefs.reload()
            return prefs.getBoolean("enabled", true)
        }

        fun log(msg: String) {
            Log.d(TAG, msg)
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != PACKAGE_SYSTEMUI) return

        if (!isEnabled()) {
            log("Module disabled in preferences, skipping hooks")
            return
        }

        log("Loading AOD Replacer hooks into SystemUI")

        AodDisablerHook.hook(lpparam)
        ScreenStateReceiver.register(lpparam)
    }
}
