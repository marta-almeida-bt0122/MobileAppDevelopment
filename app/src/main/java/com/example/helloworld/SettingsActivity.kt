package com.example.helloworld

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etUserIdentifier: EditText = findViewById(R.id.etUserIdentifier)
        val etApiKey: EditText = findViewById(R.id.editTextApiKey)
        val btnSave: Button = findViewById(R.id.btnSave)

        // Load existing values
        val savedIdentifier = loadUserIdentifier()
        val savedApiKey = loadApiKey()
        etUserIdentifier.setText(savedIdentifier)
        etApiKey.setText(savedApiKey)

        // Save button listener
        btnSave.setOnClickListener {
            val userIdentifier = etUserIdentifier.text.toString()
            val apiKey = etApiKey.text.toString()

            if (userIdentifier.isNotBlank()) {
                saveUserIdentifier(userIdentifier)
            }
            if (apiKey.isNotBlank()) {
                saveApiKey(apiKey)
            }

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        val btnBackToMain = findViewById<Button>(R.id.btnBackToMainSettings)
        btnBackToMain.setOnClickListener {
            finish()
        }
    }

    private fun saveUserIdentifier(identifier: String) {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("userIdentifier", identifier)
            apply()
        }
    }

    private fun loadUserIdentifier(): String {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userIdentifier", "") ?: ""
    }

    private fun saveApiKey(apiKey: String) {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("API_KEY", apiKey)
            apply()
        }
    }

    private fun loadApiKey(): String {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("API_KEY", "") ?: ""
    }
}


