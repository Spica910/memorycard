package com.mostree.memorycard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mostree.memorycard.R
import com.mostree.memorycard.databinding.ActivityMainBinding
import com.mostree.memorycard.db.LearningDeckDao
import com.mostree.memorycard.model.LearningDeck
import kotlinx.coroutines.launch
import android.util.Log // Added import

/**
 * The main activity of the application.
 * Displays a list of learning decks and allows users to create new decks.
 * Implements [CreateDeckDialogFragment.DeckCreationListener] to handle new deck creation events.
 */
class MainActivity : AppCompatActivity(), CreateDeckDialogFragment.DeckCreationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deckAdapter: DeckAdapter // Uses the separate DeckAdapter.kt
    private lateinit var deckDao: LearningDeckDao

    /**
     * Called when the activity is first created.
     * Initializes ViewBinding, DAOs, RecyclerView, and observers for deck data.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Data Access Object for LearningDecks
        deckDao = (application as MainApplication).database.learningDeckDao()

        // Setup RecyclerView with an adapter
        // Initially, the adapter is given an empty list.
        // The list will be populated when data is loaded from the database.
        deckAdapter = DeckAdapter(emptyList())
        binding.recyclerViewDecks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deckAdapter
        }

        // Observe changes in the list of decks from the database.
        // The collected data (a List<LearningDeck>) is submitted to the adapter to update the UI.
        lifecycleScope.launch {
            deckDao.getAllDecks().collect { decks ->
                deckAdapter.updateDecks(decks)
            }
        }

        // Setup FloatingActionButton to show the CreateDeckDialogFragment
        binding.fabAddDeck.setOnClickListener {
            CreateDeckDialogFragment().show(supportFragmentManager, CreateDeckDialogFragment.TAG)
        }
    }

    /**
     * Callback method from [CreateDeckDialogFragment.DeckCreationListener].
     * Called when a new deck is successfully created in the dialog.
     * This method handles inserting the new deck into the database.
     *
     * @param topic The topic of the newly created deck.
     */
    override fun onDeckCreated(topic: String) {
        Log.d("MainActivity", "onDeckCreated callback for topic: $topic. Deck creation is handled by DialogFragment.")
        // All other code that created and inserted a new deck should be removed.
    }
}
