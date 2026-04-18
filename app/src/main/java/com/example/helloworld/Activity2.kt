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

        try {
            val db = AppDatabase.getDatabase(this)

            // Load coordinates from Room database
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val coordinatesList = db.coordinatesDao().getAll()
                    val coordinates = coordinatesList.map { entity ->
                        "${entity.timestamp};${entity.latitude};${entity.longitude};${entity.altitude}"
                    }

                    runOnUiThread {
                        // Add header
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
                    android.util.Log.e("Activity2", "DB error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Activity2", "Init error: ${e.message}")
        }

        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)
        btnBackToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnGoToActivity3 = findViewById<Button>(R.id.btnGoToActivity3)
        btnGoToActivity3.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}






