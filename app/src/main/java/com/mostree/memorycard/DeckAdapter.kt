package com.mostree.memorycard

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mostree.memorycard.model.LearningDeck

// This is the version that was using List<String>
class DeckAdapter(private var decks: List<LearningDeck>) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeckViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]
        holder.deckNameTextView.text = deck.topic // Bind topic to TextView
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CardLearningActivity::class.java).apply {
                putExtra("DECK_ID", deck.id) // Pass actual deck ID
                putExtra("DECK_NAME", deck.topic) // Pass deck topic/name
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = decks.size

    fun updateDecks(newDecks: List<LearningDeck>) { // Changed to List<LearningDeck>
        decks = newDecks
        notifyDataSetChanged() // Consider using DiffUtil for better performance with larger lists
    }

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deckNameTextView: TextView = itemView.findViewById(android.R.id.text1)
    }
}
