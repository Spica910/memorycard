package com.mostree.memorycard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.mostree.memorycard.databinding.ItemLearningCardBinding
import com.mostree.memorycard.model.LearningCard
import kotlinx.coroutines.launch

/**
 * Adapter for displaying [LearningCard] items in a RecyclerView, typically used with [CardLearningActivity].
 * This adapter handles binding card data, managing detail view visibility, and reflecting audio playback states.
 *
 * @param cards The mutable list of [LearningCard]s to be displayed.
 * @param activity A reference to the [CardLearningActivity] to interact with its [StateFlow] for audio playback
 *                 and to call its `playAudio` method.
 */
class CardStackAdapter(
    private var cards: MutableList<LearningCard>,
    private val activity: CardLearningActivity // Reference to the activity for StateFlow and playAudio()
) : RecyclerView.Adapter<CardStackAdapter.CardViewHolder>() {

    /**
     * Creates a new [CardViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemLearningCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CardViewHolder(binding, activity)
    }

    /**
     * Binds data to the [CardViewHolder] at the given position.
     */
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        if (position < cards.size) { // Boundary check
            holder.bind(cards[position])
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    override fun getItemCount(): Int = cards.size

    /**
     * Updates the list of cards in the adapter.
     * @param newCards The new list of [LearningCard]s to display.
     */
    fun updateCards(newCards: List<LearningCard>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged() // Consider using DiffUtil for better performance with large lists and specific updates
    }

    /**
     * ViewHolder for a single [LearningCard].
     * Manages the display of card content, detail visibility, and audio playback button state.
     *
     * @param binding The ViewBinding instance for the card item layout.
     * @param activity A reference to [CardLearningActivity] to observe audio state and trigger playback.
     */
    inner class CardViewHolder(
        private val binding: ItemLearningCardBinding,
        private val activity: CardLearningActivity
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCardId: String? = null // To track the card this ViewHolder is currently bound to

        /**
         * Binds a [LearningCard] object to the ViewHolder's views.
         * Sets up text content, detail visibility toggles, audio playback button state, and click listeners.
         *
         * @param card The [LearningCard] to display.
         */
        fun bind(card: LearningCard) {
            currentCardId = card.id // Store current card ID for StateFlow observation

            // Reset visibility state for recycled views
            binding.layoutDetailedInfo.visibility = View.GONE
            binding.textViewCardText.text = card.text

            // Bind Etymology
            val etymologyAvailable = !card.etymology.isNullOrEmpty()
            binding.textViewEtymologyLabel.visibility = if (etymologyAvailable) View.VISIBLE else View.GONE
            binding.textViewEtymology.visibility = if (etymologyAvailable) View.VISIBLE else View.GONE
            binding.textViewEtymology.text = if (etymologyAvailable) card.etymology else activity.getString(R.string.info_not_available)

            // Bind Mnemonic
            val mnemonicAvailable = !card.mnemonic.isNullOrEmpty()
            binding.textViewMnemonicLabel.visibility = if (mnemonicAvailable) View.VISIBLE else View.GONE
            binding.textViewMnemonic.visibility = if (mnemonicAvailable) View.VISIBLE else View.GONE
            binding.textViewMnemonic.text = if (mnemonicAvailable) card.mnemonic else activity.getString(R.string.info_not_available)

            // Bind Examples
            val examplesAvailable = !card.examples.isNullOrEmpty()
            binding.textViewExamplesLabel.visibility = if (examplesAvailable) View.VISIBLE else View.GONE
            binding.textViewExamples.visibility = if (examplesAvailable) View.VISIBLE else View.GONE
            binding.textViewExamples.text = card.examples?.joinToString("\n\n") ?: activity.getString(R.string.examples_not_available)

            // Toggle visibility of the detailed information section
            binding.cardViewRoot.setOnClickListener {
                binding.layoutDetailedInfo.visibility =
                    if (binding.layoutDetailedInfo.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            // Setup for Play Pronunciation Button
            val audioPathAvailable = !card.pronunciationAudioPath.isNullOrEmpty()
            binding.buttonPlayPronunciation.visibility = if (audioPathAvailable) View.VISIBLE else View.GONE
            binding.buttonPlayPronunciation.isEnabled = audioPathAvailable
            binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_idle)

            // Observe audio playback state from CardLearningActivity
            itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                activity.audioPlaybackStateFlow.collect { uiState ->
                    if (uiState.cardId == currentCardId) {
                        binding.buttonPlayPronunciation.isEnabled = true
                        when (uiState.state) {
                            PlaybackState.IDLE -> binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_idle)
                            PlaybackState.BUFFERING -> binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_buffering)
                            PlaybackState.PLAYING -> binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_playing)
                            PlaybackState.ERROR -> binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_error)
                            PlaybackState.COMPLETED -> binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_idle)
                        }
                    } else {
                        binding.buttonPlayPronunciation.text = activity.getString(R.string.play_pronunciation_button_idle)
                        binding.buttonPlayPronunciation.isEnabled = audioPathAvailable
                    }
                }
            }

            // Set click listener for the play button if audio is available
            // Set click listener for the play button
            // The listener itself doesn't need to change, as enablement is handled above.
            // However, ensure it's only active if the button should be clickable.
            if (audioPathAvailable) {
                binding.buttonPlayPronunciation.setOnClickListener {
                    activity.playAudio(card.pronunciationAudioPath, card.id)
                }
            } else {
                binding.buttonPlayPronunciation.setOnClickListener(null)
            }
        }
    }
}
