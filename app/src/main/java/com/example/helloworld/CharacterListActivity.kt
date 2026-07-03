package com.example.helloworld

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.game.GameRepository
import kotlinx.coroutines.launch

class CharacterListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_character_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.listRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val listView = findViewById<ListView>(R.id.lvCharacters)
        val emptyView = findViewById<TextView>(R.id.tvEmpty)

        lifecycleScope.launch {
            val ranking = GameRepository(this@CharacterListActivity).getGlobalRanking()
            if (ranking.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                listView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                listView.visibility = View.VISIBLE
                listView.adapter = RankingAdapter(this@CharacterListActivity, ranking)
            }
        }
    }

    private class RankingAdapter(
        context: Context,
        private val items: List<GameRepository.UserPoints>
    ) : ArrayAdapter<GameRepository.UserPoints>(context, R.layout.listview_character_item, items) {

        private val inflater = LayoutInflater.from(context)
        private val medals = listOf("🥇", "🥈", "🥉")

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: inflater.inflate(R.layout.listview_character_item, parent, false)
            val item = items[position]
            val medal = medals.getOrElse(position) { "🏅" }

            view.findViewById<TextView>(R.id.tvRowEmoji).text = medal
            view.findViewById<TextView>(R.id.tvRowName).text = item.displayName
            view.findViewById<TextView>(R.id.tvRowDetails).text = "#${position + 1}"
            view.findViewById<TextView>(R.id.tvRowScore).text = item.totalPoints.toInt().toString()
            return view
        }
    }
}
