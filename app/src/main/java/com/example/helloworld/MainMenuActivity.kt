package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.game.GameRepository
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Landing screen after a successful login. Offers the 5 main actions.
 */
class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menuRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top + 16, bars.right, bars.bottom)
            insets
        }

        val user = FirebaseAuth.getInstance().currentUser
        findViewById<TextView>(R.id.tvUser).text =
            "Signed in as ${user?.displayName ?: user?.email ?: "-"}"

        findViewById<Button>(R.id.btnPlay).setOnClickListener { onPlay() }
        findViewById<Button>(R.id.btnSunscreen).setOnClickListener {
            startActivity(Intent(this, SunscreenActivity::class.java))
        }
        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            startActivity(Intent(this, CreateCharacterActivity::class.java))
        }
        findViewById<Button>(R.id.btnList).setOnClickListener {
            startActivity(Intent(this, CharacterListActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }
    }

    private fun onPlay() {
        // Check if the user has an active character; if not, redirect to Create
        val repo = GameRepository(this)
        lifecycleScope.launch {
            val active = repo.getActiveCharacter()
            if (active == null) {
                Toast.makeText(
                    this@MainMenuActivity,
                    "No active character. Create one first!",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@MainMenuActivity, CreateCharacterActivity::class.java))
            } else {
                val intent = Intent(this@MainMenuActivity, GameActivity::class.java)
                intent.putExtra("characterId", active.id)
                startActivity(intent)
            }
        }
    }

    private fun logout() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
