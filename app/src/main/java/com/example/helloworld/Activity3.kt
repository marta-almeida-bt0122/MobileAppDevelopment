package com.example.helloworld

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.room.AppDatabase
import com.example.helloworld.room.CoordinatesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Activity3 : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var timestamp: Long = 0L
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var etAltitude: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_3)

        try {
            db = AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("Activity3", "DB init error: ${e.message}")
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.thirdActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get coordinates from Intent
        timestamp = intent.getStringExtra("timestamp")?.toLong() ?: 0L
        val latitude = intent.getStringExtra("latitude") ?: "0.0"
        val longitude = intent.getStringExtra("longitude") ?: "0.0"
        val altitude = intent.getStringExtra("altitude") ?: "0.0"

        // Get EditText references
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etAltitude = findViewById(R.id.etAltitude)

        // Pre-fill with received values
        etLatitude.setText(latitude)
        etLongitude.setText(longitude)
        etAltitude.setText(altitude)

        // Update button
        val btnUpdate: Button = findViewById(R.id.btnUpdate)
        btnUpdate.setOnClickListener {
            showConfirmationDialog("Update this coordinate?") {
                updateCoordinate()
            }
        }

        // Delete button
        val btnDelete: Button = findViewById(R.id.btnDelete)
        btnDelete.setOnClickListener {
            showConfirmationDialog("Delete this coordinate?") {
                deleteCoordinate()
            }
        }

        val btnGoToSecondActivity = findViewById<Button>(R.id.btnGoToSecondActivity)
        btnGoToSecondActivity.setOnClickListener {
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
        }
    }

    private fun updateCoordinate() {
        val lat = etLatitude.text.toString().toDoubleOrNull()
        val lon = etLongitude.text.toString().toDoubleOrNull()
        val alt = etAltitude.text.toString().toDoubleOrNull()

        if (lat == null || lon == null || alt == null) {
            Toast.makeText(this, "Invalid values", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updatedEntity = CoordinatesEntity(
                    timestamp = timestamp,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt
                )
                db.coordinatesDao().updateCoordinate(updatedEntity)

                runOnUiThread {
                    Toast.makeText(this@Activity3, "Updated", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Activity3, Activity2::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("Activity3", "Update error: ${e.message}")
            }
        }
    }

    private fun deleteCoordinate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.coordinatesDao().deleteWithTimestamp(timestamp)

                runOnUiThread {
                    Toast.makeText(this@Activity3, "Deleted", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Activity3, Activity2::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("Activity3", "Delete error: ${e.message}")
            }
        }
    }

    private fun showConfirmationDialog(message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}



