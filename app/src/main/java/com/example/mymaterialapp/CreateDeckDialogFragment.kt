package com.example.mymaterialapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.mymaterialapp.databinding.DialogCreateDeckBinding
import com.example.mymaterialapp.db.LearningCardDao
import com.example.mymaterialapp.db.LearningDeckDao
import com.example.mymaterialapp.model.LearningCard
import com.example.mymaterialapp.model.LearningDeck
import com.example.mymaterialapp.network.ApiResult
import com.example.mymaterialapp.network.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateDeckDialogFragment : DialogFragment() {

    interface DeckCreationListener {
        fun onDeckCreated(topic: String) // This can still be useful for specific actions in MainActivity
    }

    private var listener: DeckCreationListener? = null
    private var _binding: DialogCreateDeckBinding? = null
    private val binding get() = _binding!!

    private lateinit var deckDao: LearningDeckDao
    private lateinit var cardDao: LearningCardDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize DAOs
        val appDatabase = (requireActivity().application as MainApplication).database
        deckDao = appDatabase.learningDeckDao()
        cardDao = appDatabase.learningCardDao()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            context as DeckCreationListener
        } catch (e: ClassCastException) {
            // Allow parentFragment to also be a listener
            parentFragment as? DeckCreationListener
        }
        // If still null, it means neither activity nor parent fragment implements it.
        // Depending on requirements, you could throw an exception here or make it optional.
        if (listener == null && context is DeckCreationListener) {
             listener = context // Should have been caught by first try if context is Activity
        } else if (listener == null && parentFragment is DeckCreationListener) {
            listener = parentFragment as DeckCreationListener
        }
        // If the listener is strictly required, you might throw:
        // if (listener == null) throw ClassCastException("$context or parentFragment must implement DeckCreationListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateDeckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCreate.setOnClickListener {
            val topic = binding.editTextTopic.text.toString().trim()
            if (topic.isNotEmpty()) {
                setLoadingState(true)

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = GeminiService.generateVocabulary(topic) // suspend call

                    // Add this logging block
                    if (topic == "토플단어 10개") {
                        Log.d("GeminiDebug", "Topic: $topic")
                        when (result) {
                            is ApiResult.Success -> {
                                Log.d("GeminiDebug", "API Success. Data: ${result.data}")
                                if (result.data.isEmpty()) {
                                    Log.w("GeminiDebug", "API returned success but data is empty for '토플단어 10개'.")
                                } else {
                                    result.data.forEachIndexed { index, pair ->
                                        Log.d("GeminiDebug", "Item $index: Word='${pair.first}', Translation='${pair.second}'")
                                    }
                                }
                            }
                            is ApiResult.Error -> {
                                Log.e("GeminiDebug", "API Error. Message: ${result.message}")
                            }
                        }
                    }

                    when (result) {
                        is ApiResult.Success -> {
                            val newDeck = LearningDeck(topic = topic)
                            val generatedItems = result.data // List<Pair<String, String>>

                            val learningCards = generatedItems.mapIndexed { index, pair ->
                                val textContent = pair.first
                                val exampleSentence = pair.second
                                var audioPath: String? = null

                                // Add sample audio URL for the first card for testing
                                if (index == 0) {
                                    audioPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                                }
                                // Optionally, add for a second card with a different URL if available for more testing
                                // else if (index == 1) {
                                //     audioPath = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
                                // }

                                LearningCard(
                                    deckId = newDeck.id,
                                    text = textContent,
                                    examples = listOf(exampleSentence),
                                    pronunciationAudioPath = audioPath // Assign the audio path
                                    // Other fields like etymology, mnemonic can be added if API provides them
                                )
                            }

                            try {
                                deckDao.insertDeck(newDeck)
                                if (learningCards.isNotEmpty()) {
                                    cardDao.insertCards(learningCards)
                                }
                                withContext(Dispatchers.Main) {
                                    val successMessage = getString(R.string.deck_topic_cards_created_success, topic, learningCards.size)
                                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show()
                                    listener?.onDeckCreated(topic)
                                    dismiss()
                                }
                            } catch (e: Exception) {
                                Log.e("DBError", "Failed to save deck/cards for topic '$topic'", e)
                                withContext(Dispatchers.Main) {
                                    val errorMessage = getString(R.string.deck_creation_db_error, e.message ?: "Unknown DB error")
                                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                                    setLoadingState(false)
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            withContext(Dispatchers.Main) {
                                val errorMessage = getString(R.string.deck_creation_api_error, result.message)
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                                setLoadingState(false)
                            }
                        }
                    }
                }
            } else {
                binding.editTextTopic.error = getString(R.string.empty_topic_error)
                if (binding.progressBarCreateDeck.visibility == View.VISIBLE) {
                    setLoadingState(false)
                }
            }
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        // Ensure operations on binding are safe if view is already destroyed
        _binding?.apply {
            progressBarCreateDeck.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonCreate.isEnabled = !isLoading
            buttonCancel.isEnabled = !isLoading
            editTextTopic.isEnabled = !isLoading
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(R.drawable.bg_glass_dialog)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important for preventing memory leaks with ViewBinding
    }

    companion object {
        const val TAG = "CreateDeckDialog"
    }
}
