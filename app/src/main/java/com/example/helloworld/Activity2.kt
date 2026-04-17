package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Activity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.secondActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val listView: ListView = findViewById(R.id.coordinatesListView)
        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)
        val btnGoToActivity3 = findViewById<Button>(R.id.btnGoToActivity3)

        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnGoToActivity3.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val db = try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("Activity2", "DB init failed: ${e.message}")
            android.util.Log.e("Activity2", e.stackTraceToString())
            return
        }

        // Load coordinates from Room database
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val coordinatesList = db.coordinatesDao().getAll()
                android.util.Log.d("Activity2", "Loaded ${coordinatesList.size} coordinates")

                val coordinates = coordinatesList.map { entity ->
                    android.util.Log.d("Activity2", "Coord: ${entity.timestamp};${entity.latitude};${entity.longitude};${entity.altitude}")
                    "${entity.timestamp};${entity.latitude};${entity.longitude};${entity.altitude}"
                }

                runOnUiThread {
                    if (coordinates.isEmpty()) {
                        android.util.Log.d("Activity2", "No coordinates found")
                        val emptyView = android.widget.TextView(this@Activity2)
                        emptyView.text = "No coordinates saved yet.\n\nEnable location tracking on main screen to start saving locations."
                        emptyView.gravity = android.view.Gravity.CENTER
                        emptyView.setPadding(16, 16, 16, 16)
                        emptyView.textSize = 16f
                        listView.emptyView = emptyView
                        (listView.parent as android.view.ViewGroup).addView(emptyView)
                    }

                    val headerView = layoutInflater.inflate(R.layout.listview_header, listView, false)
                    listView.addHeaderView(headerView, null, false)

                    val adapter = CoordinatesAdapter(this@Activity2, coordinates)
                    listView.adapter = adapter

                    // Handle item clicks
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val adjustedPosition = position - 1
                        if (adjustedPosition >= 0 && adjustedPosition < coordinates.size) {
                            val item = coordinates[adjustedPosition]
                            val parts = item.split(";")
                            if (parts.size >= 4) {
                                android.util.Log.d("Activity2", "Clicked: $item")
                                val intent = Intent(this@Activity2, Activity3::class.java)
                                intent.putExtra("timestamp", parts[0])
                                intent.putExtra("latitude", parts[1])
                                intent.putExtra("longitude", parts[2])
                                intent.putExtra("altitude", parts[3])
                                startActivity(intent)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Activity2", "Data load error: ${e.message}")
                android.util.Log.e("Activity2", e.stackTraceToString())
                runOnUiThread {
                    android.widget.Toast.makeText(this@Activity2, "Error loading coordinates", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}






