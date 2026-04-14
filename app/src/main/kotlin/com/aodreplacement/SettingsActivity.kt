package com.aodreplacement

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use world-readable prefs so XSharedPreferences can read them from the hook
        preferenceManager.sharedPreferencesName = "module_prefs"
        preferenceManager.sharedPreferencesMode = Context.MODE_WORLD_READABLE

        @Suppress("DEPRECATION")
        addPreferencesFromResource(R.xml.prefs)
    }
}
