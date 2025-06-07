package com.example.mymaterialapp

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymaterialapp.databinding.ActivityCardLearningBinding
import com.example.mymaterialapp.db.LearningCardDao
import com.example.mymaterialapp.db.LearningDeckDao
import com.example.mymaterialapp.model.LearningCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Enum representing the possible states of audio playback.
 */
enum class PlaybackState {
    /** Initial state, no audio loaded or playback finished/stopped. */
    IDLE,
    /** Audio data is being loaded/buffered. */
    BUFFERING,
    /** Audio is currently playing. */
    PLAYING,
    /** An error occurred during playback. */
    ERROR,
    /** Playback has completed successfully. */
    COMPLETED
}

/**
 * Data class representing the UI state for audio playback, including the card ID,
 * current playback state, the audio path, and any error message.
 *
 * @property cardId The ID of the card associated with this audio state. Null if no specific card.
 * @property state The current [PlaybackState].
 * @property audioPath The path/URL of the audio being played or loaded.
 * @property errorMessage An optional error message if the state is [PlaybackState.ERROR].
 */
data class AudioPlaybackUiState(
    val cardId: String? = null,
    val state: PlaybackState = PlaybackState.IDLE,
    val audioPath: String? = null,
    val errorMessage: String? = null
)

/**
 * Activity for learning cards within a deck.
 * Manages card display using a RecyclerView, swipe gestures for marking cards,
 * and audio playback for pronunciations.
 */
class CardLearningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardLearningBinding
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var cardAdapter: CardStackAdapter
    private lateinit var cardDao: LearningCardDao
    private lateinit var deckDao: LearningDeckDao
    private var currentCards: MutableList<LearningCard> = mutableListOf()
    private var currentDeckId: String? = null

    private val _audioPlaybackStateFlow = MutableStateFlow(AudioPlaybackUiState())
    val audioPlaybackStateFlow: StateFlow<AudioPlaybackUiState> = _audioPlaybackStateFlow.asStateFlow()

    private var playbackCompletionJob: Job? = null

    /**
     * Initializes the activity, sets up UI, loads deck information and cards for learning.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeDaos()
        setupRecyclerView()
        loadDeckAndCards()
        setupItemTouchHelper()
        updateCardPositionDisplay() // Initial display based on potentially empty list
    }

    private fun initializeDaos() {
        val appDatabase = (application as MainApplication).database
        cardDao = appDatabase.learningCardDao()
        deckDao = appDatabase.learningDeckDao()
    }

    private fun setupRecyclerView() {
        cardAdapter = CardStackAdapter(currentCards, this) // Pass activity reference
        binding.recyclerViewCards.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerViewCards.adapter = cardAdapter
    }

    private fun loadDeckAndCards() {
        currentDeckId = intent.getStringExtra("DECK_ID")
        val deckNameFromIntent = intent.getStringExtra("DECK_NAME") ?: getString(R.string.loading_deck_text)
        binding.textViewDeckName.text = deckNameFromIntent

        currentDeckId?.let { deckId ->
            lifecycleScope.launch {
                val deck = deckDao.getDeckById(deckId)
                deck?.let { binding.textViewDeckName.text = it.topic }
            }

            lifecycleScope.launch {
                cardDao.getUnlearnedCardsForDeck(deckId).collect { unlearnedCardsFromDb ->
                    currentCards.clear()
                    currentCards.addAll(unlearnedCardsFromDb)
                    cardAdapter.updateCards(unlearnedCardsFromDb)
                    updateCardPositionDisplay()
                }
            }
        } ?: run {
            Toast.makeText(this, getString(R.string.deck_id_not_found_error), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Sets up the ItemTouchHelper for handling swipe gestures on RecyclerView items.
     * Left swipe marks a card as known.
     * Right swipe marks a card as unknown and moves it to the end of the session list.
     */
    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, // No drag directions
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Swipe left and right
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false // Drag and drop not used

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= currentCards.size) {
                    cardAdapter.notifyItemChanged(position) // Reset swipe visual
                    return
                }

                val swipedCard = currentCards.removeAt(position)
                cardAdapter.notifyItemRemoved(position)

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        swipedCard.isKnown = true
                        Log.d("SwipeAction", "Card '${swipedCard.text}' marked as KNOWN.")
                    }
                    ItemTouchHelper.RIGHT -> {
                        swipedCard.isKnown = false
                        currentCards.add(swipedCard) // Add to end of the current session's list
                        cardAdapter.notifyItemInserted(currentCards.size - 1)
                        Log.d("SwipeAction", "Card '${swipedCard.text}' marked as UNKNOWN and moved to session end.")
                    }
                }

                lifecycleScope.launch {
                    cardDao.updateCard(swipedCard) // Persist change to DB
                }
                updateCardPositionDisplay()
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewCards)
    }

    /**
     * Updates the TextView that displays the current card position (e.g., "Card 1 of 10").
     */
    private fun updateCardPositionDisplay() {
        if (currentCards.isEmpty()) {
            binding.textViewCardPosition.text = getString(R.string.no_cards_left_text)
        } else {
            binding.textViewCardPosition.text = getString(R.string.card_position_format, 1, currentCards.size)
        }
    }

    /**
     * Plays audio from the given URL for the specified card ID.
     * Manages MediaPlayer state and updates [audioPlaybackStateFlow].
     *
     * @param requestedAudioPath The URL of the audio to play.
     * @param requestedCardId The ID of the card whose audio is being requested.
     */
    fun playAudio(requestedAudioPath: String?, requestedCardId: String) {
        playbackCompletionJob?.cancel()
        val currentState = _audioPlaybackStateFlow.value

        if (currentState.cardId == requestedCardId &&
            (currentState.state == PlaybackState.PLAYING || currentState.state == PlaybackState.BUFFERING)) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.IDLE, requestedAudioPath)
            Log.d("MediaPlayer", "Playback stopped by user for card $requestedCardId.")
            return
        }

        if (mediaPlayer?.isPlaying == true || currentState.state == PlaybackState.BUFFERING) {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
        }

        if (requestedAudioPath.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.audio_path_not_available), Toast.LENGTH_SHORT).show()
            _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.ERROR, null, getString(R.string.audio_path_not_available_error_detail))
            return
        }

        _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.BUFFERING, requestedAudioPath)
        Toast.makeText(this, getString(R.string.audio_buffering_message), Toast.LENGTH_SHORT).show()

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }

        mediaPlayer?.apply {
            reset() // Reset before setting new data source
            setOnPreparedListener { mp ->
                _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.PLAYING, requestedAudioPath)
                mp.start()
                Log.d("MediaPlayer", "Audio prepared, starting playback for card $requestedCardId.")
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Error for card $requestedCardId. What: $what, Extra: $extra")
                _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.ERROR, requestedAudioPath, getString(R.string.audio_playback_error))
                reset()
                true
            }
            setOnCompletionListener {
                Log.d("MediaPlayer", "Playback completed for card $requestedCardId.")
                _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.COMPLETED, requestedAudioPath)
                playbackCompletionJob = lifecycleScope.launch {
                    delay(200) // Short delay before resetting to IDLE
                    if (_audioPlaybackStateFlow.value.cardId == requestedCardId && _audioPlaybackStateFlow.value.state == PlaybackState.COMPLETED) {
                        _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.IDLE, requestedAudioPath)
                    }
                }
            }
            try {
                setDataSource(requestedAudioPath)
                prepareAsync() // Prepare asynchronously
                Log.d("MediaPlayer", "Preparing audio for card $requestedCardId: $requestedAudioPath")
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Error setting data source or preparing for card $requestedCardId", e)
                _audioPlaybackStateFlow.value = AudioPlaybackUiState(requestedCardId, PlaybackState.ERROR, requestedAudioPath, e.message ?: getString(R.string.audio_setup_error_detail))
                reset()
            }
        }
    }

    /**
     * Releases MediaPlayer resources when the activity is stopped.
     */
    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _audioPlaybackStateFlow.value = AudioPlaybackUiState() // Reset global audio state
        playbackCompletionJob?.cancel() // Cancel any pending completion job
        Log.d("MediaPlayer", "MediaPlayer released and state reset onStop.")
    }
}
