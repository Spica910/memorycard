package com.mostree.memorycard.network

import android.util.Log
import com.example.mymaterialapp.BuildConfig // To access GEMINI_API_KEY
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
     * @return An [ApiResult] containing a list of pairs (word/phrase to example sentence) on success,
     *         or an [ApiResult.Error] on failure or if an error is simulated.
     */
    suspend fun generateVocabulary(topic: String): ApiResult<List<Pair<String, String>>> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d(MOCK_TAG, "Generating MOCK vocabulary for topic: $topic. API Key (first 5 chars): ${apiKey.take(5)}...")

        // Simulate network delay
        delay(1500) // milliseconds

        // Mock data generation
        val mockData = listOf(
            Pair("$topic - Word 1 (Mock)", "This is a mock example sentence for Word 1 related to $topic."),
            Pair("$topic - Phrase 1 (Mock)", "A common mock phrase about $topic to learn."),
            Pair("$topic - Word 2 (Mock)", "Another mock word for $topic with its example."),
            Pair("$topic - Idiom 1 (Mock)", "Mock idioms can also be about $topic."),
            Pair("$topic - Word 3 (Mock)", "Final mock word for $topic in this list.")
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
    private fun parseVocabularyResponse(responseText: String): List<Pair<String, String>> {
        val items = mutableListOf<Pair<String, String>>()
        responseText.lines().forEach { line ->
            if (line.isNotBlank()) {
                val parts = line.split("###", limit = 2)
                if (parts.size == 2) {
                    items.add(Pair(parts[0].trim(), parts[1].trim()))
                } else {
                    Log.w(MOCK_TAG, "Could not parse line from response: $line")
                }
            }
        }
        return items
    }
}
