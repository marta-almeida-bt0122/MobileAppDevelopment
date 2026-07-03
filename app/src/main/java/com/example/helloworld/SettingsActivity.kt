package com.example.helloworld

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

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

        val etDisplayName = findViewById<EditText>(R.id.etDisplayName)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)

        etDisplayName.setText(FirebaseAuth.getInstance().currentUser?.displayName ?: "")

        findViewById<Button>(R.id.btnSaveName).setOnClickListener {
            saveDisplayName(etDisplayName.text.toString().trim())
        }
        findViewById<Button>(R.id.btnSavePassword).setOnClickListener {
            savePassword(etNewPassword.text.toString())
        }
        findViewById<Button>(R.id.btnBackToMainSettings).setOnClickListener {
            finish()
        }
    }

    private fun saveDisplayName(name: String) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val update = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(update)
            .addOnSuccessListener {
                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not update name: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePassword(password: String) {
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        user.updatePassword(password)
            .addOnSuccessListener {
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not update password: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
