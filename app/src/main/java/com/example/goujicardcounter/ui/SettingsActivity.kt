// SettingsActivity.kt: Settings activity for app configuration
package com.example.goujicardcounter.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.goujicardcounter.R

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        supportActionBar?.title = "设置"
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let {
            when (it) {
                "key_ocr_confidence" -> Toast.makeText(this, "OCR识别阈值已更新", Toast.LENGTH_SHORT).show()
                "key_recognition_interval" -> Toast.makeText(this, "识别间隔已更新", Toast.LENGTH_SHORT).show()
                "key_auto_start" -> Toast.makeText(this, "自动启动设置已更新", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}
