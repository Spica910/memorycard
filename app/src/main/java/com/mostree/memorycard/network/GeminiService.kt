package com.mostree.memorycard.network

import android.util.Log
import com.mostree.memorycard.BuildConfig // To access GEMINI_API_KEY
import com.mostree.memorycard.model.DetailedVocabularyItem // Import new data class
import kotlinx.coroutines.delay // For simulating network delay with mock data

/**
 * Sealed class representing the result of an API operation.
 * It can be either [ApiResult.Success] or [ApiResult.Error].
 *
 * @param T The type of the data in case of success.
 */
sealed class ApiResult<out T> {
    /**
     * Represents a successful API operation.
     * @param data The data returned by the API.
     */
    data class Success<out T>(val data: T) : ApiResult<T>()

    /**
     * Represents a failed API operation.
     * @param message A descriptive error message.
     * @param cause An optional [Exception] that caused the error.
     */
    data class Error(val message: String, val cause: Exception? = null) : ApiResult<Nothing>()
}

/**
 * Service object for interacting with the Gemini API (currently mocked).
 * Provides methods to generate vocabulary and other content based on topics.
 */
object GeminiService {

    private const val MOCK_TAG = "GeminiServiceMock"

    /**
     * Generates a list of vocabulary items (word/phrase and example sentence) based on the given topic.
     *
     * NOTE: This implementation currently returns MOCK data and simulates a network delay.
     * The API key from [BuildConfig] is logged for verification but not used in the mock call.
     *
     * TODO: Replace with actual Gemini API call when the client library and setup are finalized.
     *       This will involve using an appropriate API client, handling authentication with the API key,
     *       making the network request, and parsing the actual response.
     *
     * @param topic The topic for which to generate vocabulary.
     * @return An [ApiResult] containing a list of [DetailedVocabularyItem] on success,
     *         or an [ApiResult.Error] on failure or if an error is simulated.
     */
    suspend fun generateVocabulary(topic: String): ApiResult<List<DetailedVocabularyItem>> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d(MOCK_TAG, "Generating MOCK vocabulary for topic: $topic. API Key (first 5 chars): ${apiKey.take(5)}...")

        // Simulate network delay
        delay(1500) // milliseconds

        // Mock data generation
        val mockData = listOf(
            DetailedVocabularyItem(
                text = "$topic - Word 1 (Mock)",
                example = "This is a mock example sentence for Word 1 related to $topic.",
                etymology = "Mock etymology for Word 1: from Old Mockish 'wrd'.",
                mnemonic = "Mock mnemonic for Word 1: think of a 'word' in a 'mocking' bird's song."
            ),
            DetailedVocabularyItem(
                text = "$topic - Phrase 1 (Mock)",
                example = "A common mock phrase about $topic to learn.",
                etymology = "Mock etymology for Phrase 1: combination of 'phras' and 'mock'.",
                mnemonic = "Mock mnemonic for Phrase 1: 'phrase' it like you 'mock' it."
            ),
            DetailedVocabularyItem(
                text = "$topic - Word 2 (Mock)",
                example = "Another mock word for $topic with its example.",
                etymology = "Mock etymology for Word 2: from Medieval Mock 'woord'.",
                mnemonic = "Mock mnemonic for Word 2: two 'words' make a 'mock'."
            ),
            DetailedVocabularyItem(
                text = "$topic - Idiom 1 (Mock)",
                example = "Mock idioms can also be about $topic.",
                etymology = "Mock etymology for Idiom 1: 'idio' + 'mock' meaning peculiar mockery.",
                mnemonic = "Mock mnemonic for Idiom 1: 'idiots' sometimes 'mock' things."
            ),
            DetailedVocabularyItem(
                text = "$topic - Word 3 (Mock)",
                example = "Final mock word for $topic in this list.",
                etymology = "Mock etymology for Word 3: from High Mock 'wort'.",
                mnemonic = "Mock mnemonic for Word 3: three 'mocks' a charm for this 'word'."
            ),
            DetailedVocabularyItem(
                text = "$topic - Word 4 (Mock)",
                example = "Example sentence for Word 4 about $topic.",
                etymology = "Mock etymology for Word 4: from Proto-Mockish 'werd'.",
                mnemonic = "Mock mnemonic for Word 4: 'four' 'words' to 'mock' it up."
            ),
            DetailedVocabularyItem(
                text = "$topic - Phrase 2 (Mock)",
                example = "Another phrase related to $topic for learning.",
                etymology = "Mock etymology for Phrase 2: see Phrase 1, but doubled.",
                mnemonic = "Mock mnemonic for Phrase 2: second 'phrase', double the 'mock'."
            ),
            DetailedVocabularyItem(
                text = "$topic - Word 5 (Mock)",
                example = "Mock word 5 and its usage in $topic.",
                etymology = "Mock etymology for Word 5: from Low Mock 'wurd'.",
                mnemonic = "Mock mnemonic for Word 5: high 'five' for this 'mock' 'word'."
            ),
            DetailedVocabularyItem(
                text = "$topic - Idiom 2 (Mock)",
                example = "More mock idioms concerning $topic.",
                etymology = "Mock etymology for Idiom 2: variant of Idiom 1.",
                mnemonic = "Mock mnemonic for Idiom 2: another 'idiom' to 'mock'."
            ),
            DetailedVocabularyItem(
                text = "$topic - Word 6 (Mock)",
                example = "The sixth mock word for $topic.",
                etymology = "Mock etymology for Word 6: numeral 'six' + 'wrd'.",
                mnemonic = "Mock mnemonic for Word 6: 'six' 'words' a 'mocking'."
            )
        )

        // Example of how to simulate an error for testing purposes:
        // if (topic.equals("error_test", ignoreCase = true)) {
        //     Log.w(MOCK_TAG, "Simulating API error for topic '$topic'.")
        //     // For user-facing errors, consider using string resources if the message is static.
        //     // Here, the message includes the dynamic 'topic', so it's constructed.
        //     // The calling code (e.g., DialogFragment) should use string resources for the generic part of the error display.
        //     return ApiResult.Error("Simulated API error for topic '$topic'.")
        // }

        Log.d(MOCK_TAG, "Successfully generated MOCK data for topic: $topic")
        return ApiResult.Success(mockData)
    }

    /**
     * Parses a raw text response (assumed to be from an API) into a list of word/example pairs.
     * This function assumes a specific format where each line is "word/phrase ### definition/example".
     *
     * NOTE: Not directly used by the current mock implementation of [generateVocabulary] but
     *       is kept as a utility for potential future use with a real API response.
     *
     * @param responseText The raw text response from the API.
     * @return A list of pairs, where each pair contains a word/phrase and its corresponding definition/example.
     */
    @Suppress("unused") // Suppressed because it's not used by the current mock logic
    private fun parseVocabularyResponse(responseText: String): List<DetailedVocabularyItem> {
        // This function would need to be updated if it were to parse into DetailedVocabularyItem
        // For now, its signature is changed to match the new expectation, but the logic is not used.
        val items = mutableListOf<DetailedVocabularyItem>()
        responseText.lines().forEach { line ->
            if (line.isNotBlank()) {
                val parts = line.split("###", limit = 4) // Assuming format: text###example###etymology###mnemonic
                if (parts.size == 4) {
                    items.add(DetailedVocabularyItem(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()))
                } else if (parts.size == 2) { // Fallback for old format or missing optional fields
                     items.add(DetailedVocabularyItem(parts[0].trim(), parts[1].trim()))
                }
                else {
                    Log.w(MOCK_TAG, "Could not parse line into DetailedVocabularyItem: $line")
                }
            }
        }
        return items
    }
}
