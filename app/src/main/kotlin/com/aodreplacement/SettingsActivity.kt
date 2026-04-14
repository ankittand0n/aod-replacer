package com.aodreplacement

import android.os.Bundle
import android.preference.PreferenceActivity

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceManager.sharedPreferencesName = "module_prefs"

        @Suppress("DEPRECATION")
        addPreferencesFromResource(R.xml.prefs)
    }
}
