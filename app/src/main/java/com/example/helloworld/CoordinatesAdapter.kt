package com.example.helloworld

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CoordinatesAdapter(
    context: Context,
    private val coordinates: List<String>
) : ArrayAdapter<String>(context, 0, coordinates) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.listview_item, parent, false)

        val item = coordinates[position]
        val parts = item.split(";")

        if (parts.size >= 4) {
            val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
            val tvLatitude: TextView = view.findViewById(R.id.tvLatitude)
            val tvLongitude: TextView = view.findViewById(R.id.tvLongitude)
            val tvAltitude: TextView = view.findViewById(R.id.tvAltitude)

            val timestamp = try {
                val timeInMs = parts[0].toLong()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timeInMs))
            } catch (e: Exception) {
                parts[0]
            }

            tvTimestamp.text = timestamp
            tvLatitude.text = parts[1]
            tvLongitude.text = parts[2]
            tvAltitude.text = parts[3]
        }

        return view
    }
}

