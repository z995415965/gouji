// SettingsFragment.kt: Fragment for settings UI
package com.example.goujicardcounter.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.goujicardcounter.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Setup preference listeners
        findPreference<androidx.preference.SwitchPreference>("key_auto_start")?.setOnPreferenceChangeListener { _, newValue ->
            // Handle auto-start toggle
            true
        }

        findPreference<androidx.preference.EditTextPreference>("key_ocr_confidence")?.setOnPreferenceChangeListener { _, newValue ->
            try {
                val confidence = newValue.toString().toFloat()
                if (confidence in 0.0f..1.0f) true else {
                    androidx.appcompat.app.AlertDialog.makeText(context, "置信度需在0-1之间", androidx.appcompat.app.AlertDialog.LENGTH_SHORT).show()
                    false
                }
            } catch (e: NumberFormatException) {
                false
            }
        }
    }
}
