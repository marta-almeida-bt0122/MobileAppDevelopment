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
import com.example.helloworld.game.GameEngine
import com.example.helloworld.game.GameRepository
import com.example.helloworld.room.CharacterEntity
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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
            val ranking = GameRepository(this@CharacterListActivity).getRanking()
            if (ranking.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                listView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                listView.visibility = View.VISIBLE
                listView.adapter = CharacterAdapter(this@CharacterListActivity, ranking)
            }
        }
    }

    private class CharacterAdapter(
        context: Context,
        private val items: List<CharacterEntity>
    ) : ArrayAdapter<CharacterEntity>(context, R.layout.listview_character_item, items) {

        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: inflater.inflate(R.layout.listview_character_item, parent, false)
            val item = items[position]
            val skin = GameEngine.skinTypeById(item.skinType)
            val ageDays = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - item.createdAt
            )
            val status = if (item.alive) "alive" else "rest in peace 💀"

            view.findViewById<TextView>(R.id.tvRowEmoji).text = skin.emoji
            view.findViewById<TextView>(R.id.tvRowName).text = item.name
            view.findViewById<TextView>(R.id.tvRowDetails).text =
                "${skin.label} · $ageDays days · $status"
            view.findViewById<TextView>(R.id.tvRowScore).text = item.score.toInt().toString()
            return view
        }
    }
}
