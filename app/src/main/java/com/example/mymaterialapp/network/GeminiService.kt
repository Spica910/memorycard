package com.example.mymaterialapp.network

import android.util.Log
import com.example.mymaterialapp.BuildConfig // To access GEMINI_API_KEY
import com.google.ai.client.generativeai.GenerativeModel
// Consider adding import com.google.ai.client.generativeai.type.GenerateContentResponse (if accessing specific response fields beyond .text)
// import com.google.ai.client.generativeai.type.generationBlockedError (if specific structured error handling is needed)
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Data class to hold the parsed components from the API response before mapping to LearningCard.
 */
data class CardPrototype(
    val word: String,
    val translation: String,
    val example: String
)

/**
 * Service object for interacting with the Gemini API.
 * Provides methods to generate vocabulary and other content based on topics.
 */
object GeminiService {

    private const val SERVICE_TAG = "GeminiService"

    /**
     * Generates a list of vocabulary items (word, translation, example sentence) based on the given topic
     * using the Gemini API.
     *
     * @param topic The topic for which to generate vocabulary.
     * @return An [ApiResult] containing a list of [CardPrototype] objects on success,
     *         or an [ApiResult.Error] on failure.
     */
    suspend fun generateVocabulary(topic: String): ApiResult<List<CardPrototype>> {
        Log.d(SERVICE_TAG, "Requesting vocabulary for topic: $topic. API Key (first 5 chars): ${BuildConfig.GEMINI_API_KEY.take(5)}...")

        val generativeModel = GenerativeModel(
            modelName = "gemini-pro", // Or "gemini-1.0-pro" or other suitable model
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        val prompt = """
            Generate a list of 5 English vocabulary words, their Korean translations, and an example sentence in English for each, related to the topic: '$topic'.
            Format each item strictly as:
            English Word ### Korean Translation ### English Example Sentence
            Provide only the list, one item per line. Do not include any introductory text, concluding text, numbering, or bullet points.
            For example, if the topic is 'technology':
            Innovation ### 혁신 ### Regular software updates drive technological innovation.
            Algorithm ### 알고리즘 ### Search engines use complex algorithms to rank pages.
        """.trimIndent()

        Log.d(SERVICE_TAG, "Prompt for topic '$topic':\n$prompt")

        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text
                Log.d(SERVICE_TAG, "Raw API response for topic '$topic':\n$responseText")

                if (responseText != null) {
                    val parsedData = parseActualApiResponse(responseText)
                    if (parsedData.isNotEmpty()) {
                        Log.i(SERVICE_TAG, "Successfully parsed ${parsedData.size} CardPrototype items for topic: $topic")
                        ApiResult.Success(parsedData)
                    } else {
                        Log.w(SERVICE_TAG, "API response parsed to empty list of CardPrototype for topic: $topic. Response: $responseText")
                        ApiResult.Error("No vocabulary data received from API or failed to parse to CardPrototype for topic: $topic. Response: $responseText")
                    }
                } else {
                    Log.e(SERVICE_TAG, "API response text is null for topic: $topic. Full response object: $response")
                    val blockReason = response.promptFeedback?.blockReason?.name
                    val finishReason = response.candidates.firstOrNull()?.finishReason?.name
                    val safetyRatings = response.promptFeedback?.safetyRatings?.joinToString { "${it.category}: ${it.probability}" }
                    val errorDetails = "Block reason: $blockReason, Finish reason: $finishReason, Safety ratings: $safetyRatings"
                    ApiResult.Error("Received empty response from API for topic: $topic. Details: $errorDetails")
                }
            } catch (e: Exception) {
                Log.e(SERVICE_TAG, "API call failed for topic: $topic", e)
                ApiResult.Error("API call failed for topic: $topic. Error: ${e.message}", e)
            }
        }
    }

    /**
     * Parses a raw text response from the Gemini API into a list of [CardPrototype] objects.
     * Assumes each relevant line in the responseText is formatted as:
     * "English Word ### Korean Translation ### English Example Sentence"
     *
     * @param responseText The raw text response from the API.
     * @return A list of [CardPrototype] objects.
     */
    private fun parseActualApiResponse(responseText: String): List<CardPrototype> {
        val items = mutableListOf<CardPrototype>()
        responseText.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotBlank()) {
                val parts = trimmedLine.split("###", limit = 3)
                if (parts.size == 3) {
                    val word = parts[0].trim()
                    val translation = parts[1].trim()
                    val example = parts[2].trim()
                    if (word.isNotEmpty() && translation.isNotEmpty() && example.isNotEmpty()) {
                        items.add(CardPrototype(word, translation, example))
                        Log.d(SERVICE_TAG, "Parsed CardPrototype: Word='$word', Translation='$translation', Example='$example'")
                    } else {
                        Log.w(SERVICE_TAG, "Skipping line due to empty parts after parsing (word, translation, or example): '$trimmedLine'")
                    }
                } else {
                    Log.w(SERVICE_TAG, "Could not parse line into 3 parts for CardPrototype (expected 'Word ### Translation ### Example'): '$trimmedLine'")
                }
            }
        }
        if (items.isEmpty() && responseText.isNotBlank() && !responseText.contains("###")) {
            Log.w(SERVICE_TAG, "Parsing CardPrototype resulted in an empty list. The response text was not blank and did not contain '###', indicating a possible fundamental format mismatch from the API. Response:\n$responseText")
        } else if (items.isEmpty() && responseText.isNotBlank()) {
             Log.w(SERVICE_TAG, "Parsing CardPrototype resulted in an empty list, but the response text was not blank. This might indicate a format mismatch (e.g. wrong delimiter or structure) from the API. Response:\n$responseText")
        }
        return items
    }
}
