package com.example.helloworld

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.room.AppDatabase
import com.example.helloworld.room.CoordinatesEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Activity3 : AppCompatActivity() {

    private val TAG = "Activity3"
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
            Log.e(TAG, "DB init error: ${e.message}")
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.thirdActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        timestamp = intent.getStringExtra("timestamp")?.toLong() ?: 0L
        val latitude = intent.getStringExtra("latitude") ?: "0.0"
        val longitude = intent.getStringExtra("longitude") ?: "0.0"
        val altitude = intent.getStringExtra("altitude") ?: "0.0"

        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etAltitude = findViewById(R.id.etAltitude)

        etLatitude.setText(latitude)
        etLongitude.setText(longitude)
        etAltitude.setText(altitude)

        findViewById<Button>(R.id.btnUpdate).setOnClickListener {
            showConfirmationDialog("Update this coordinate?") { updateCoordinate() }
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            showConfirmationDialog("Delete this coordinate?") { deleteCoordinate() }
        }

        findViewById<Button>(R.id.btnGoToSecondActivity).setOnClickListener {
            startActivity(Intent(this, Activity2::class.java))
        }

        // Firebase: Add Report button
        val editTextReport: EditText = findViewById(R.id.editTextReport)
        val addReportButton: Button = findViewById(R.id.addReportButton)
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        addReportButton.setOnClickListener {
            val reportText = editTextReport.text.toString().trim()
            if (reportText.isNotEmpty() && userId != null) {
                val report = mapOf(
                    "userId" to userId,
                    "timestamp" to timestamp,
                    "report" to reportText,
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                addReportToDatabase(report)
            } else {
                Toast.makeText(this, "Report name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addReportToDatabase(report: Map<String, Any>) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("hotspots").push()
        databaseReference.setValue(report)
            .addOnSuccessListener {
                Toast.makeText(this, "Report added successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Report saved to Firebase: $report")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add report: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Firebase error: ${e.message}")
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
                db.coordinatesDao().updateCoordinate(CoordinatesEntity(timestamp, lat, lon, alt))
                runOnUiThread {
                    Toast.makeText(this@Activity3, "Updated", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Activity3, Activity2::class.java))
                }
            } catch (e: Exception) { Log.e(TAG, "Update error: ${e.message}") }
        }
    }

    private fun deleteCoordinate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.coordinatesDao().deleteWithTimestamp(timestamp)
                runOnUiThread {
                    Toast.makeText(this@Activity3, "Deleted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@Activity3, Activity2::class.java))
                }
            } catch (e: Exception) { Log.e(TAG, "Delete error: ${e.message}") }
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
